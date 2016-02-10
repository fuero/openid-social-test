package org.example.openid_social_test;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.social.security.SocialUserDetails;
import org.springframework.social.security.SocialUserDetailsService;

public class SocialUserDetailService implements SocialUserDetailsService {

	@Override
	public SocialUserDetails loadUserByUserId(String userId) throws UsernameNotFoundException {
		return new SocialUserDetails() {
			private static final long serialVersionUID = -8226846383294090446L;
			@Override
			public boolean isEnabled() {
				return true;
			}
			@Override
			public boolean isCredentialsNonExpired() {
				return false;
			}
			@Override
			public boolean isAccountNonLocked() {
				return false;
			}
			@Override
			public boolean isAccountNonExpired() {
				return false;
			}
			@Override
			public String getUsername() {
				return userId;
			}
			@Override
			public String getPassword() {
				return null;
			}
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				Set<GrantedAuthority> auth = Collections.emptySet();
				auth.add(new SimpleGrantedAuthority("ROLE_USER"));
				return auth;
			}
			@Override
			public String getUserId() {
				return userId;
			}
		};
	}

}
