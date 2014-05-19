package com.github.anastasop.koskino;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Score {
	private byte[] scoreHashCode;
	
	private Score(byte[] scoreHashCode) {
		this.scoreHashCode = scoreHashCode;
	}
	
	public static Score forBlock(byte[] b) {
	  MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      // "Every implementation of the Java platform is
      //required to support the following standard MessageDigest algorithms"
      // md5, sha1, sha256
      e.printStackTrace();
      throw new InternalError(e);
    }
	  md.update(b);
	  byte[] hash = md.digest();
		return new Score(hash);
	}
	
	public static Score fromBytes(byte[] b) {
		return new Score(b);
	}
	
	public byte[] getBytes() {
		return scoreHashCode;
	}
	
	final private static char[] hexArray = "0123456789abcdef".toCharArray();
	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	@Override
	public String toString() {
		return bytesToHex(scoreHashCode);
	}
}
