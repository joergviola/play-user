package controllers;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


import notifiers.Mails;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import com.google.gson.JsonObject;

import play.Logger;
import play.Play;
import play.libs.Mail;
import play.modules.facebook.FbGraph;
import play.modules.facebook.FbGraphException;
import play.mvc.Http.Header;
import play.mvc.results.Result;
import play.utils.Java;

public class Security extends controllers.Secure.Security {
	
	static public class UserProvider {

		public static boolean authenticate(String username, String password) {
			return true;
		}

		public static boolean check(String profile) {
			return true;
		}
		
		public static void create(String email, String activation, String password, String facebookId) {
			
		}

		public static boolean activate(String email, String activation) {
			return true;
		}

        private static Object invoke(String m, Object... args) throws Throwable {
            Class userProvider = null;
            List<Class> classes = Play.classloader.getAssignableClasses(UserProvider.class);
            if(classes.size() == 0) {
                userProvider = UserProvider.class;
            } else {
                userProvider = classes.get(0);
            }
            try {
                return Java.invokeStaticOrParent(userProvider, m, args);
            } catch(InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

	}
	private static String hash(String input) {
		try {
			java.security.MessageDigest d = java.security.MessageDigest
					.getInstance("SHA-1");
			d.reset();
			d.update(input.getBytes());
			byte[] digest = d.digest();
			return new String(Hex.encodeHex(digest));
		} catch (NoSuchAlgorithmException e) {
			Logger.warn("SHA-1 not available");
			return input;
		}
	}

	static boolean authenticate(String username, String password) {
		try {
			return (Boolean) UserProvider.invoke("authenticate", username, hash(password));
		} catch (Throwable e) {
			Logger.error(e, "On authenticate");
			throw new RuntimeException(e);
		}
	}

	static boolean check(String profile) {
		try {
			return (Boolean) UserProvider.invoke("check", profile);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	static void onAuthenticated() {
		redirect("/");
	}

	static void onDisconnected() {
		redirect("/");
	}
	

	public static void login(String email, String password) {
		Header referer = request.headers.get("referer");
		try {
			boolean ok = (Boolean) UserProvider.invoke("authenticate", email, hash(password));
			if (ok) 
				session.put("username", email);
			redirect(referer.value());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void facebookLogin() {
		Header referer = request.headers.get("referer");
        try {
            JsonObject profile = FbGraph.getObject("me"); // fetch the logged in user
            String fbid = profile.get("id").getAsString(); // retrieve the user id
            String email = profile.get("email").getAsString(); // retrieve the email
			UserProvider.invoke("create", email, "", fbid);
            session.put("username", email); // put the email into the session (for the Secure module)
        } catch (FbGraphException fbge) {
            flash.error(fbge.getMessage());
            if (fbge.getType() != null && fbge.getType().equals("OAuthException")) {
                session.remove("username");
            }
		} catch (Throwable e) {
			throw new RuntimeException(e);
        }
		redirect(referer.value());
	}

	public static void register(String email, String password, String password2) {
		Header referer = request.headers.get("referer");
		if (!StringUtils.equals(password, password2)) {
			redirect(referer.value());
		} else {
			try {
				String activation = RandomStringUtils.randomAlphabetic(20);
				boolean ok = (Boolean) UserProvider.invoke("create", email, activation , hash(password), "");
				if (ok)
					session.put("username", email);
				String from = "from";
				String subject = "subject";
				Mails.activation(from, email,subject,  activation);
				redirect(referer.value());
			} catch (Result r) {
				throw r;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void activate(String email, String code) {
		try {
			boolean ok = (Boolean) UserProvider.invoke("activate", email, code);
			if (ok)
				redirect("/");
			else 
				render();
		} catch (Result r) {
			throw r;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
