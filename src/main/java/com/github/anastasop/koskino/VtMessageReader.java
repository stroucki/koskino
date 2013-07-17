package com.github.anastasop.koskino;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedBytes;


public class VtMessageReader {
	private InputStream ist;
	private int nBytesOfLength;
	private byte[] buf;

	public VtMessageReader(InputStream ist) {
		this.ist = ist;
		this.nBytesOfLength = 2;
		this.buf = new byte[65536 * 2];
	}
	
	public void setProtocolVersion(String protocolVersion) throws ProtocolException {
		switch(protocolVersion) {
		case "02":
			this.nBytesOfLength = 2;
			break;
		case "04":
			this.nBytesOfLength = 4;
			break;
		default:
			throw new ProtocolException("Unsupported protocol " + protocolVersion);
		}
	}
	
	public VtMessage read() throws ProtocolException, IOException {
		int nBytesOfLengthRead = ByteStreams.read(ist, buf, 0, nBytesOfLength);
		// if no bytes are read, we assume the connection has closed.
		if (nBytesOfLengthRead == 0) {
			return null;
		}
		// else we assume a protocol error
		if (nBytesOfLengthRead != nBytesOfLength) {
			throw new ProtocolException("cannot read message length");
		}
		int messageLength = 0;
		for (int i = 0; i < nBytesOfLength; i++) {
			messageLength <<= 8;
			messageLength |= UnsignedBytes.toInt(buf[i]);
		}
		if (messageLength == 0) {
			throw new ProtocolException("premature end of message");
		}
		if (messageLength > buf.length) {
			throw new ProtocolException("VtMessage too long. Accepts up to " + buf.length);
		}
		if (ByteStreams.read(ist, buf, 0, messageLength) != messageLength) {
			throw new ProtocolException("premature end of message");
		}
		
		
		
		VtMessage msg = new VtMessage();
		VtMessageScanner scanner = new VtMessageScanner(buf, 0, messageLength);
		Integer messageType = scanner.readCount(1);
		if (messageType == null) {
			throw new ProtocolException("Malformed message: no message type");
		}
		Integer tag = scanner.readCount(1);
		if (tag == null) {
			throw new ProtocolException("Malformed message: no message tag");
		}
		Integer type = null, pad = null;
		switch (messageType.byteValue()) {
		case VtMessage.VtThello:
			// VtThello tag[1] version[s] uid[s] strength[1] crypto[n] codec[n]
			String version = scanner.readString();
			String uid = scanner.readString();
			Integer strength = scanner.readCount(1);
			String crypto = scanner.readDatum();
			String codec = scanner.readDatum();
			if (version == null || uid == null ||
					strength == null || crypto == null || codec == null) {
				throw new ProtocolException("Malformed VtHello message");
			}
			msg.msgType = VtMessage.VtThello;
			msg.tag = tag.byteValue();
			msg.version = version;
			msg.uid = uid;
			msg.strength = tag.byteValue();
			msg.crypto = crypto;
			msg.codec = codec;
			break;
		case VtMessage.VtTread:
			// VtTread tag[1] score[20] type[1] pad[1] count[2]
			byte[] score = scanner.readScore();
			type = scanner.readCount(1);
			pad = scanner.readCount(1);
			Integer count = scanner.readCount(nBytesOfLength);
			if (score == null || type == null || pad == null || count == null) {
				throw new ProtocolException("Malformed VtTread message");
			}
			msg.msgType = VtMessage.VtTread;
			msg.tag = tag.byteValue();
			msg.score = Score.fromBytes(score);
			msg.count = count;
			msg.type = type.byteValue();
			msg.pad = pad;
			break;
		case VtMessage.VtTwrite:
			// VtTwrite tag[1] type[1] pad[3] data[]
			type = scanner.readCount(1);
			pad = scanner.readCount(3);
			byte[] data = null;
			int dataLength = messageLength - 6;
			if (dataLength > 0) {
				data = new byte[dataLength];
				System.arraycopy(buf, 6, data, 0, dataLength);
			} else {
				data = new byte[0];
			}
			if (type == null || pad == null || data == null) {
				throw new ProtocolException("Malformed VtTwrite message");
			}
			msg.msgType = VtMessage.VtTwrite;
			msg.tag = tag.byteValue();
			msg.data = data;
			msg.type = type.byteValue();
			msg.pad = pad;
			break;
		case VtMessage.VtTsync:
		case VtMessage.VtTping:
		case VtMessage.VtTgoodbye:
			msg.msgType = messageType.byteValue();
			msg.tag = tag.byteValue();
			break;
			
		case VtMessage.VtTauth0:
		case VtMessage.VtTauth1:
			throw new ProtocolException("Cannot understand Tauth{0,1} messages");
			
		case VtMessage.VtRerror:
		case VtMessage.VtRping:
		case VtMessage.VtRhello:
		case VtMessage.VtRgoodbye:
		case VtMessage.VtRauth0:
		case VtMessage.VtRauth1:
		case VtMessage.VtRread:
		case VtMessage.VtRwrite:
		case VtMessage.VtRsync:
			throw new ProtocolException("MessageReader can read only TMessages");
		default:
			throw new ProtocolException("unknown message type " + messageType);
		}
		return msg;
	}
}
