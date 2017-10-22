package com.netease.qa.emmagee.utils;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Customized Authenticator
 * 
 * @author andrewleo
 */
public class CustomizedAuthenticator extends Authenticator {
	String userName = null;
	String password = null;

	public CustomizedAuthenticator() {
	}

	public CustomizedAuthenticator(String username, String password) {
		this.userName = username;
		this.password = password;
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(userName, password);
	}
}
