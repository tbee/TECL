package org.tbee.tecl;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

public class EncryptionHelper {

	static EncryptionHelper me = new EncryptionHelper();

	/**
	 * This class can be called directly to encode a value 
	 * @param args no args generates a key pair for usage with this method, 2 args
	 * @throws NoSuchAlgorithmException 
	 */
	static public void main(String[] args) throws NoSuchAlgorithmException {
		
		// Usage
		if (args.length != 1 && args.length != 2) {
			System.out.println("Usage: <to-encrypt> <key-in-base64> ");
			System.out.println("Or: <size> returns a key pair generated with the size (recommended: 2048)");
			System.exit(1);
		}
		
		// Key pair
		if (args.length == 1) {
			me.generateKeyPair(args);
			System.exit(0);
		}
		
		// Encode
		System.out.print(me.encode(args[0], args[1]));
	}

	private void generateKeyPair(String[] args) throws NoSuchAlgorithmException {
		
		// Generate pair
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(Integer.parseInt(args[0]));
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		String publicBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
		String privateBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
		
		// Test it
		String decoded = "Some text " + System.currentTimeMillis();
		String encoded = encode(decoded, publicBase64);
		if (encoded.equals(decoded)) throw new IllegalArgumentException("Encode did not encode");
		String decodedAgain = decode(encoded, privateBase64);
		if (!decodedAgain.equals(decoded)) throw new IllegalArgumentException("Encode-decode did not return the same result.");
		
		System.out.println("Public key base64 encoded: " + publicBase64);
		System.out.println("Private key base64 encoded: " + privateBase64);
		System.out.println("Use the public key to encode with, and use the private key to decode with.");
		
	}
	
	/**
	 * 
	 * @param decoded Decoded (readable) text
	 * @param keyBase64 Can be both private or public, as long as it is the opposite of what was used to encode
	 * @return
	 */
    String encode(String decoded, String keyBase64) {
    	try {
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	
	        // Get public key
	        X509EncodedKeySpec publicKeyEncoded = new X509EncodedKeySpec(Base64.getDecoder().decode(keyBase64));
	        PublicKey publicKey = keyFactory.generatePublic(publicKeyEncoded);
	
	        // Encrypt message
	        Cipher cipher = Cipher.getInstance("RSA");  
	        cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
	        byte[] encryptedBytes = cipher.doFinal(decoded.getBytes());  

	        // Base 64
	        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);	       
	        return encryptedBase64;
    	}
    	catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * 
     * @param encryptedBase64 Encoded (unreadable) text
     * @param keyBase64 Can be both private or public, as long as it is the opposite of what was used to encode
     * @return
     */
    String decode(String encryptedBase64, String keyBase64) {
    	try {
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    	
	        // make PrivateKey
	        PKCS8EncodedKeySpec privateKeyEncoded = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBase64));
	        PrivateKey privateKey = keyFactory.generatePrivate(privateKeyEncoded);
	        
	        // DeBase64
	        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
	        
	        // Decrypt
	        Cipher cipher = Cipher.getInstance("RSA");  
	        cipher.init(Cipher.DECRYPT_MODE, privateKey);	    
	        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	        String decrypted = new String(decryptedBytes);	        
	        return decrypted;
    	}
    	catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
}
