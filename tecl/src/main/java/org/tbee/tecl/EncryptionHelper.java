package org.tbee.tecl;

/*-
 * #%L
 * TECL
 * %%
 * Copyright (C) 2020 Tom Eugelink
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;

public class EncryptionHelper {

	static EncryptionHelper me = new EncryptionHelper();

	/**
	 * This class can be called directly to encode a value 
	 * @param args no args generates a key pair for usage with this method, 2 args
	 * @throws NoSuchAlgorithmException 
	 */
	static public void main(String[] args) throws NoSuchAlgorithmException {
		
		if (args.length == 2 && "keypair".equals(args[0])) {
			int keySize = Integer.parseInt(args[1]);
			me.generateKeyPair(keySize);
			System.exit(0);
		}
		if (args.length == 3 && "encrypt".contentEquals(args[0])) {
			System.out.print(me.encode(args[1], args[2]));
			System.exit(0);
		}
		
		System.out.println("Usage: ");
		System.out.println("  encrypt <text-to-encrypt> <key-in-base64> ");
		System.out.println("  keypair <size> // returns a key pair generated (recommended: 2048)");
		System.exit(1);
	}

	List<String> generateKeyPair(int keySize) throws NoSuchAlgorithmException {
		
		// Generate pair
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(keySize);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		String publicBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
		String privateBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
		
		// Done
		System.out.println("Public key base64 encoded: " + publicBase64);
		System.out.println("Private key base64 encoded: " + privateBase64);
		return Arrays.asList(publicBase64, privateBase64);
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
