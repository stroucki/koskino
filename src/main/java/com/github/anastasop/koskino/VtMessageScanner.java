package com.github.anastasop.koskino;

import java.io.UnsupportedEncodingException;

import com.google.common.primitives.UnsignedBytes;

class VtMessageScanner {
	private byte[] buf;
	private int len;
	private int pos;
	
	public VtMessageScanner(byte[] buf, int off, int len) {
		this.buf = buf;
		this.len = len;
		this.pos = off;
	}
	
	public String readString() {
		String s = null;
		if (pos + 1 < len) {
			int b0 = UnsignedBytes.toInt(buf[pos]);
			int b1 = UnsignedBytes.toInt(buf[pos + 1]);
			int slen = (b0 << 8) + b1;
			if (pos + 1 + slen < len) {
				try {
					s = new String(buf, pos + 2, slen, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new Error(e);
				}
			}
			pos += 2 + slen;
		}
		return s;
	}
	
	public String readDatum() {
		String s = null;
		if (pos < len) {
			int b0 = UnsignedBytes.toInt(buf[pos]);
			int slen = (b0 << 8);
			if (pos + slen < len) {
				try {
					s = new String(buf, pos + 1, slen, "ISO-8859-1");
				} catch (UnsupportedEncodingException e) {
					throw new Error(e);
				}
			}
			pos += 1 + slen;
		}
		return s;
	}
	
	public byte[] readScore() {
		byte[] b = null;
		if (pos + 20 < len) {
			b = new byte[20];
			System.arraycopy(buf, pos, b, 0, 20);
			pos += 20;
		}
		return b;
	}
	
	public Integer readCount(int nbytes) {
		Integer i = null;
		int b0, b1, b2, b3;
		if (pos + nbytes - 1 < len) {
			if (nbytes > 4) {
				nbytes = 4;
			}
			switch (nbytes) {
			case 1:
				i = UnsignedBytes.toInt(buf[pos]);
				break;
			case 2:
				b0 = UnsignedBytes.toInt(buf[pos]);
				b1 = UnsignedBytes.toInt(buf[pos + 1]);
				i = (b0 << 8) + b1;
				break;
			case 3: // to support pad
				b0 = UnsignedBytes.toInt(buf[pos]);
				b1 = UnsignedBytes.toInt(buf[pos + 1]);
				b2 = UnsignedBytes.toInt(buf[pos + 1]);
				i = (b0 << 16) + (b1 << 8) + b2;
				break;
			case 4:
				b0 = UnsignedBytes.toInt(buf[pos]);
				b1 = UnsignedBytes.toInt(buf[pos + 1]);
				b2 = UnsignedBytes.toInt(buf[pos + 2]);
				b3 = UnsignedBytes.toInt(buf[pos + 3]);
				i = (b0 << 24) + (b1 << 16) + (b2 << 8) + b3;
				break;
			}
			pos += nbytes;
		}
		return i;
	}
}
