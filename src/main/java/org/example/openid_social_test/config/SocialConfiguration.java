package org.example.openid_social_test.config;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.openid_social_test.SocialUserDetailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.openid.connect.OpenIdConnectionFactory;
import org.springframework.social.openid.security.OpenIdAuthenticationService;
import org.springframework.social.openid.security.OpenIdSocialAuthenticationFilter;
import org.springframework.social.security.AuthenticationNameUserIdSource;
import org.springframework.social.security.SocialAuthenticationServiceLocator;
import org.springframework.social.security.SocialAuthenticationServiceRegistry;
import org.springframework.social.security.SocialUserDetailsService;
import org.springframework.social.security.SpringSocialConfigurer;
import org.springframework.web.context.request.NativeWebRequest;

@Configuration
@EnableSocial
@Order(2)
public class SocialConfiguration extends WebSecurityConfigurerAdapter implements SocialConfigurer {
	private static final Log log = LogFactory.getLog(SocialConfiguration.class); 
	
	@Inject
	private Environment env;
	
	@Inject
	private DataSource dataSource;
	
	@Inject
	private TextEncryptor textEncryptor;
	
	@Inject
	private AuthenticationManager authenticationManager;
	
	@Bean
	public SocialUserDetailsService socialUserDetailsService() {
		return new SocialUserDetailService();
	}
	
    @Bean
    public SocialAuthenticationServiceLocator socialAuthenticationServiceLocator() {
    	SocialAuthenticationServiceRegistry registry = new SocialAuthenticationServiceRegistry();
    	
    	registry.addAuthenticationService(openIdAuthenticationService());
    	
    	return registry;
    }
    
    @Bean
    @Scope(value="session", proxyMode=ScopedProxyMode.INTERFACES)
    public ConnectionRepository connectionRepository() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Unable to get a ConnectionRepository: no user signed in");
        }
        log.trace("Authentication: "+authentication);
        return usersConnectionRepository().createConnectionRepository(authentication.getName());
    }

    @Bean
    public OpenIdAuthenticationService openIdAuthenticationService() {
    	return new OpenIdAuthenticationService(openIdConnectionFactory());
    }
    @Bean
    OpenIdConnectionFactory openIdConnectionFactory() {
    	return new OpenIdConnectionFactory();
    }
    
    @Bean
    public UsersConnectionRepository usersConnectionRepository() {
        JdbcUsersConnectionRepository repo = new JdbcUsersConnectionRepository(dataSource, socialAuthenticationServiceLocator(), 
            textEncryptor);
        return repo;
    }
    
    @Bean
    public ConnectController connectController() {
    	ConnectController controller = new ConnectController(socialAuthenticationServiceLocator(), connectionRepository());
        controller.setApplicationUrl(env.getProperty("application.url"));
        return controller;
    }
    @Bean
    public ProviderSignInController providerSignInController() {
    	ProviderSignInController signin = new ProviderSignInController(
    			socialAuthenticationServiceLocator(),
    			usersConnectionRepository(),
    			new SignInAdapter() {
    				private Log log = LogFactory.getLog(getClass());
					@Override
					public String signIn(String userId, Connection<?> connection, NativeWebRequest request) {
						log.debug(userId);
						SecurityContextHolder.getContext().setAuthentication(
					            new UsernamePasswordAuthenticationToken(userId, null, null));
						return null;
					}
				});
        return signin;
    }

    @Bean
    public UserIdSource userIdSource() {
    	return new AuthenticationNameUserIdSource();
    }
    
    @Bean
    public TextEncryptor textEncryptor() {
    	return Encryptors.noOpText();
    }
    
    @Bean
    public OpenIdSocialAuthenticationFilter openIdSocialAuthenticationFilter() throws Exception {
    	return new OpenIdSocialAuthenticationFilter(authenticationManager, userIdSource(), usersConnectionRepository(),
    			socialAuthenticationServiceLocator());
    }

	@Override
	public UserIdSource getUserIdSource() {
		return userIdSource();
	}

	@Override
	public UsersConnectionRepository getUsersConnectionRepository(
			ConnectionFactoryLocator connectionFactoryLocator) {
		return usersConnectionRepository();
	}

	// Hack to use ConnectionFactoryConfigurer
	@Override
	public void addConnectionFactories(
			ConnectionFactoryConfigurer connectionFactoryConfigurer,
			Environment environment) {
//		try {
//			Method m = connectionFactoryConfigurer.getClass().getMethod("getConnectionFactoryLocator", (Class[]) null);
//			m.setAccessible(true);
//			SocialAuthenticationServiceRegistry reg = (SocialAuthenticationServiceRegistry) m.invoke(connectionFactoryConfigurer, (Object[]) null);
//			reg.addAuthenticationService(openIdAuthenticationService());
//		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//			log.error("Error calling method via reflection", e);
//		}
//		
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.apply(new SpringSocialConfigurer());
	}
}
