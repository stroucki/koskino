package com.github.anastasop.koskino;

public class VtMessage {
	public static final byte VtRerror = 1;
	public static final byte VtTping = 2;
	public static final byte VtRping = 3;
	public static final byte VtThello = 4;
	public static final byte VtRhello = 5;
	public static final byte VtTgoodbye = 6;
	public static final byte VtRgoodbye = 7;
	public static final byte VtTauth0 = 8;
	public static final byte VtRauth0 = 9;
	public static final byte VtTauth1 = 10;
	public static final byte VtRauth1 = 11;
	public static final byte VtTread = 12;
	public static final byte VtRread = 13;
	public static final byte VtTwrite = 14;
	public static final byte VtRwrite = 15;
	public static final byte VtTsync = 16;
	public static final byte VtRsync = 17;
	public static final byte VtTmax = 18;
	
	public byte msgType;
	public byte tag;
	public int pad;
	public byte type;
	public String error;
	public String version;
	public String uid;
	public byte strength;
	public String crypto;
	public String codec;
	public String sid;
	public String auth;
	public Score score;
	public byte blockType;
	public int count;
	public byte[] data;
	
	public VtMessage() {}
	
	public VtMessage(byte msgType, byte tag) {
		this.msgType = msgType;
		this.tag = tag;
	}
	
	@Override
	public String toString() {
		String s = null;
		switch (msgType) {
		case VtMessage.VtRerror:
			s = String.format("VtRerror:%d:%s", tag, error);
			break;
		case VtMessage.VtTping:
			s = String.format("VtTping:%d:%s", tag);
			break;
		case VtMessage.VtRping:
			s = String.format("VtRping:%d:%s", tag);
			break;
		case VtMessage.VtThello:
			s = String.format("VtThello:%d:%s:%s:(strength)%d:(crypto)%s:(codec)%s", tag, version, uid, strength, crypto, codec);
			break;
		case VtMessage.VtRhello:
			s = String.format("VtRhello:%d:(sid)%s:(rcrypto)%d:(rcodec)%d", tag, sid, 0, 0);
			break;
		case VtMessage.VtTgoodbye:
			s = String.format("VtTgoodbye:%d", tag);
			break;
		case VtMessage.VtRgoodbye:
			s = String.format("VtRgoodbye:%d", tag);
			break;
		case VtMessage.VtTauth0:
			s = String.format("VtTauth0:%d", tag);
			break;
		case VtMessage.VtRauth0:
			s = String.format("VtRauth0:%d", tag);
			break;
		case VtMessage.VtTauth1:
			s = String.format("VtTauth1:%d", tag);
			break;
		case VtMessage.VtRauth1:
			s = String.format("VtRauth1:%d", tag);
			break;
		case VtMessage.VtTread:
			s = String.format("VtTread:%d:%s:(type)%d:(pad)%d:(count)%d", tag, score, type, pad, count);
			break;
		case VtMessage.VtRread:
			s = String.format("VtRread:%d:(count of data)%d", tag, data.length);
			break;
		case VtMessage.VtTwrite:
			s = String.format("VtTwrite:%d:(type)%d:(pad)%d:(count of data)%d", tag, type, pad, data.length);
			break;
		case VtMessage.VtRwrite:
			s = String.format("VtRwrite:%d:%s", tag, score);
			break;
		case VtMessage.VtTsync:
			s = String.format("VtTsync:%d", tag);
			break;
		case VtMessage.VtRsync:
			s = String.format("VtRsync:%d", tag);
			break;
		default:
			s = String.format("Unknown message:%d", msgType);
			break;
		}
		return s;
	}
}
