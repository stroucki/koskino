package com.github.anastasop.koskino;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class VtMessageSerializer {
	private static final Charset UTF8Charset = Charset.forName("UTF-8");
	private static final Charset Latin1Charset = Charset.forName("ISO-8859-1");
	
	private String protocolVersion;
	
	public void setProtocolVersion(String protocolVersion) throws ProtocolException {
		this.protocolVersion = protocolVersion;
	}
	
	public VtMessageSerializer() {
		this.protocolVersion = "02";
	}
	
	private void writeMessageLength(int length, OutputStream ost) throws IOException {
		switch (protocolVersion) {
		case "04":
			writeCount(4, length, ost);
			break;
		default:
			writeCount(2, length, ost);
			break;
		}
	}
	
	private void writeByte(int b, OutputStream ost) throws IOException {
		ost.write(b);
	}
	
	private void writeBytes(byte[] b, OutputStream ost) throws IOException {
		ost.write(b);
	}
	
	private void writeCount(int nbytes, int count, OutputStream ost) throws IOException {
		int b0, b1, b2, b3;
		if (nbytes > 4) {
			nbytes = 4;
		}
		switch (nbytes) {
		case 1:
			ost.write(count);
		case 2:
			b0 = (count >> 8) & 0xFF;
			b1 = (count) & 0xFF;
			ost.write(b0);
			ost.write(b1);
			break;
		case 3:
			b0 = (count >> 16) & 0xFF;
			b1 = (count >> 8) & 0xFF;
			b2 = (count) & 0xFF;
			ost.write(b0);
			ost.write(b1);
			ost.write(b2);
			break;
		case 4:
			b0 = (count >> 24) & 0xFF;
			b1 = (count >> 16) & 0xFF;
			b2 = (count >> 8) & 0xFF;
			b3 = (count) & 0xFF;
			ost.write(b0);
			ost.write(b1);
			ost.write(b2);
			ost.write(b3);
			break;
		}
	}
	
	private void writeScore(Score score, OutputStream ost) throws IOException {
		ost.write(score.getBytes());
	}
	
	private int len(byte[] b) {
		return b == null? 0: b.length;
	}
	
	public void writeMessage(VtMessage msg, OutputStream ost) throws IOException {
		switch (msg.msgType) {
		case VtMessage.VtRerror:
			byte[] errorString = msg.error.getBytes(UTF8Charset);
			writeMessageLength(1 + 1 + 2 + len(errorString), ost);
			writeByte(VtMessage.VtRerror, ost);
			writeByte(msg.tag, ost);
			writeCount(2, errorString.length, ost);
			writeBytes(errorString, ost);
			break;
		case VtMessage.VtTping:
			writeMessageLength(2, ost);
			writeByte(VtMessage.VtTping, ost);
			writeByte(msg.tag, ost);
			break;
		case VtMessage.VtRping:
			writeMessageLength(2, ost);
			writeByte(VtMessage.VtRping, ost);
			writeByte(msg.tag, ost);
			break;
		case VtMessage.VtThello:
			byte[] versionString = msg.version.getBytes(UTF8Charset);
			byte[] uidString = msg.uid.getBytes("UTF-8");
			byte[] cryptoDatum = msg.uid.getBytes(Latin1Charset);
			byte[] codecDatum = msg.uid.getBytes(Latin1Charset);
			writeMessageLength(1 + 1 + 2 + len(versionString) + 2 + len(uidString) + 1 + len(cryptoDatum) + 1 + len(codecDatum), ost);
			writeByte(VtMessage.VtThello, ost);
			writeByte(msg.tag, ost);
			writeCount(2, len(versionString), ost);
			writeBytes(versionString, ost);
			writeCount(2, len(uidString), ost);
			writeBytes(uidString, ost);
			writeByte(msg.strength, ost);
			writeCount(1, len(cryptoDatum), ost);
			writeBytes(cryptoDatum, ost);
			writeCount(1, len(codecDatum), ost);
			writeBytes(codecDatum, ost);
			break;
		case VtMessage.VtRhello:
			byte[] sidString = msg.sid.getBytes(UTF8Charset);
			writeMessageLength(1 + 1 + 2 + len(sidString) + 2, ost);
			writeByte(VtMessage.VtRhello, ost);
			writeByte(msg.tag, ost);
			writeCount(2, len(sidString), ost);
			writeBytes(sidString, ost);
			writeByte(0, ost);
			writeByte(0, ost);
			break;
		case VtMessage.VtTgoodbye:
			writeMessageLength(2, ost);
			writeByte(VtMessage.VtTgoodbye, ost);
			writeByte(msg.tag, ost);
			break;
		case VtMessage.VtTread:
			writeMessageLength(1 + 1 + 20 + 1 + 1 + 2, ost);
			writeByte(VtMessage.VtTread, ost);
			writeByte(msg.tag, ost);
			writeScore(msg.score, ost);
			writeByte(0, ost);
			writeByte(0, ost);
			writeCount(2, msg.count, ost);
			break;
		case VtMessage.VtRread:
			writeMessageLength(1 + 1 + len(msg.data), ost);
			writeByte(VtMessage.VtRread, ost);
			writeByte(msg.tag, ost);
			ost.write(msg.data);
			break;
		case VtMessage.VtTwrite:
			writeMessageLength(1 + 1 + 1 + 3 + len(msg.data), ost);
			writeByte(VtMessage.VtTwrite, ost);
			writeByte(msg.tag, ost);
			writeByte(0, ost);
			writeCount(3, 0, ost);
			ost.write(msg.data);
			break;
		case VtMessage.VtRwrite:
			writeMessageLength(1 + 1 + 20, ost);
			writeByte(VtMessage.VtRwrite, ost);
			writeByte(msg.tag, ost);
			writeScore(msg.score, ost);
			break;
		case VtMessage.VtTsync:
			writeMessageLength(2, ost);
			writeByte(VtMessage.VtTsync, ost);
			writeByte(msg.tag, ost);
			break;
		case VtMessage.VtRsync:
			writeMessageLength(2, ost);
			writeByte(VtMessage.VtRsync, ost);
			writeByte(msg.tag, ost);
			break;
			
		case VtMessage.VtRgoodbye:
		case VtMessage.VtTauth0:
		case VtMessage.VtRauth0:
		case VtMessage.VtTauth1:
		case VtMessage.VtRauth1:
			throw new IllegalArgumentException("cannot handle this message");
			
		default:
			throw new IllegalArgumentException("cannot understand this message");
		}
	}
}
