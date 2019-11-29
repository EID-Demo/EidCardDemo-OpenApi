package com.sunmi.readidcardemo.utils;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class ByteUtils {

    /**
     * 打印内容
     */
    public static String byte2Hex(byte[] raw, int offset, int count) {
        if (raw == null) {
            return null;
        }
        if (offset < 0 || offset > raw.length) {
            offset = 0;
        }
        int end = offset + count;
        if (end > raw.length) {
            end = raw.length;
        }
        count = end - offset;
        StringBuilder hex = new StringBuilder(count * 3);
        int h = 0, l = 0;
        for (int i = offset; i < end; i++) {
            h = raw[i] >> 4 & 0x0f;
            l = raw[i] & 0x0f;
            hex.append((char) (h > 9 ? h - 10 + 'A' : h + '0'));
            hex.append((char) (l > 9 ? l - 10 + 'A' : l + '0'));
            hex.append(' ');
        }
        if (hex.length() > 0) {
            hex.deleteCharAt(hex.length() - 1);
        }
        return hex.toString();
    }

    /**
     * 将无符号short转换成int，大端模式(高位在前)
     */
    public static int unsignedShort2IntBE(byte[] src, int offset) {
        return (src[offset] & 0xff) << 8 | (src[offset + 1] & 0xff);
    }

    /**
     * 将无符号short转换成int，小端模式(低位在前)
     */
    public static int unsignedShort2IntLE(byte[] src, int offset) {
        return (src[offset] & 0xff) | (src[offset + 1] & 0xff) << 8;
    }

    /**
     * 将无符号byte转换成int
     */
    public static int unsignedByte2Int(byte[] src, int offset) {
        return src[offset] & 0xFF;
    }

    /**
     * 将字节数组转换成int,小端模式(低位在前)
     */
    public static int unsignedInt2IntLE(byte[] src, int offset) {
        int value = 0;
        for (int i = offset; i < offset + 4; i++) {
            value |= (src[i] & 0xff) << (i - offset) * 8;
        }
        return value;
    }

    /**
     * 将字节数组转换成int,大端模式(高位在前)
     */
    public static int unsignedInt2IntBE(byte[] src, int offset) {
        int result = 0;
        for (int i = offset; i < offset + 4; i++) {
            result |= (src[i] & 0xff) << (offset + 3 - i) * 8;
        }
        return result;
    }

    /**
     * 将int转换成byte数组，大端模式(高位在前)
     */
    public static byte[] int2BytesBE(int src) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (src >> (3 - i) * 8);
        }
        return result;
    }

    /**
     * 将short转换成byte数组，大端模式(高位在前)
     */
    public static byte[] short2BytesBE(short src) {
        byte[] result = new byte[2];
        for (int i = 0; i < 2; i++) {
            result[i] = (byte) (src >> (1 - i) * 8);
        }
        return result;
    }

    /**
     * 将short转换成byte数组，小端模式(低位在前)
     */
    public static byte[] short2BytesLE(short src) {
        byte[] result = new byte[2];
        for (int i = 0; i < 2; i++) {
            result[i] = (byte) (src >> i * 8);
        }
        return result;
    }

    /**
     * 将字节数组列表合并成单个字节数组
     */
    public static byte[] concatByteArrays(byte[]... list) {
        if (list == null || list.length == 0) {
            return new byte[0];
        }
        return concatByteArrays(Arrays.asList(list));
    }

    /**
     * 将字节数组列表合并成单个字节数组
     */
    public static byte[] concatByteArrays(List<byte[]> list) {
        if (list == null || list.isEmpty()) {
            return new byte[0];
        }
        int totalLen = 0;
        for (byte[] b : list) {
            if (b == null || b.length == 0) {
                continue;
            }
            totalLen += b.length;
        }
        byte[] result = new byte[totalLen];
        int index = 0;
        for (byte[] b : list) {
            if (b == null || b.length == 0) {
                continue;
            }
            System.arraycopy(b, 0, result, index, b.length);
            index += b.length;
        }
        return result;
    }

    /**
     * 将命令码转换成16进制字符串(4位)
     */
    public static String getHexCmd(int cmd) {
        return String.format("0x%04X", cmd);
    }

    /**
     * 将命令码转换成16进制字符串(2位)
     */
    public static String getDownloadHexCmd(byte cmd) {
        return String.format("0x%02X", cmd);
    }

    /**
     * 获取LRC
     */
    public static byte genLRC(byte[] data, int offset, int len) {
        if (data == null || data.length == 0) {
            return 0;
        }
        if (offset < 0 || offset >= data.length) {
            offset = 0;
        }
        if (len < 0 || len > data.length) {
            len = data.length;
        }
        int end = offset + len;
        if (end > data.length) {
            end = data.length;
        }
        byte lrc = 0;
        for (int i = offset; i < end; i++) {
            lrc ^= data[i];
        }
        return lrc;
    }

    /**
     * 将字节数组转换成16进制字符串
     *
     * @param src 源字节数组
     * @return 转换后的16进制字符串
     */
    public static String bytes2HexString(byte... src) {
        if (src == null || src.length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : src) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Hex字符串转成字节数组
     *
     * @param hexString Hex字符串
     * @return 转换后的字节数组
     */
    public static byte[] hexString2Bytes(String hexString) {
        if (TextUtils.isEmpty(hexString)) {
            return new byte[0];
        }
        int length = hexString.length() / 2;
        char[] chars = hexString.toCharArray();
        byte[] b = new byte[length];
        int pos = 0;
        for (int i = 0; i < length; i++) {
            pos = i * 2;
            b[i] = (byte) (char2Byte(chars[pos]) << 4 | char2Byte(chars[pos + 1]));
        }
        return b;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static int char2Byte(char c) {
        if (c >= 'a') {
            return (c - 'a' + 10) & 0x0f;
        }
        if (c >= 'A') {
            return (c - 'A' + 10) & 0x0f;
        }
        return (c - '0') & 0x0f;
    }

    /** 将BCD字节数据转换为ASCII字节数组 */
    public static byte[] Bcd2Asc(byte[] a) {
        if (a == null || a.length == 0) {
            return null;
        }
        byte[] result = new byte[a.length * 2];
        for (int i = 0; i < a.length; i++) {
            byte h = (byte) (a[i] >> 4 & 0x0F);
            byte l = (byte) (a[i] & 0x0F);
            result[i * 2] = (byte) (h > 9 ? h + 'A' - 10 : h + '0');
            result[i * 2 + 1] = (byte) (l > 9 ? l + 'A' - 10 : l + '0');
        }
        return result;
    }

    /**
     * 将String转换为SP字节数组，增加结尾\0符
     *
     * @param src 源字符串
     * @return 转换后的字节数组
     */
    public static byte[] string2SPBytes(String src) {
        byte[] in = (src == null ? "".getBytes() : src.getBytes());
        byte[] out = new byte[in.length + 1];//增加一个结尾符字节
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }


    /**
     * 将SP字节数组转换成ASCII字符串
     *
     * @param src 源字节数组，以'\0'结尾
     * @return 转换后的字符串
     */
    public static String spBytes2String(byte[] src) {
        if (src == null || src.length == 0) {
            return "";
        }
        int index = src.length - 1;
        while (index >= 0 && src[index] == 0) {
            index--;
        }
        if (index < 0) {//所有字节全为0
            return "";
        }
        if (index == src.length - 1) {//src不包含\0符,添加\0符
            src = Arrays.copyOf(src, src.length + 1);
        }

        return new String(src, 0, index + 1);
    }

    /**
     * 将SP字节数组转换成Hex字符串
     *
     * @param src 源字节数组，以'\0'结尾
     * @return 转换后的字符串
     */
    public static String spBytes2HexString(byte[] src) {
        String ascii = spBytes2String(src);
        return bytes2HexString(ascii.getBytes());
    }

    /** 将ASCII 转换为BCD */
    public static byte[] ascii2bcd(byte[] ascii, int len) {
        byte[] bcd = new byte[len / 2];
        for (int i = 0; i < bcd.length; i++) {
            byte h = asc2bcd(ascii[i * 2]);
            byte l = asc2bcd(ascii[i * 2 + 1]);
            bcd[i] = (byte) (h << 4 | l);
        }
        return bcd;
    }

    /** 单个ascii字符转换成bcd值 */
    private static byte asc2bcd(byte asc) {
        if (asc >= '0' && asc <= '9') {
            return (byte) (asc - '0');
        } else if (asc >= 'A' && asc <= 'F') {
            return (byte) (asc - 'A' + 10);
        } else if (asc >= 'a' && asc <= 'f') {
            return (byte) (asc - 'a' + 10);
        }
        return (byte) (asc - 48);
    }

    public static byte[] asciiStr2Bytes(String ascii) {
        byte[] dat = null;
        try {
            dat = ascii.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return dat;
    }

    public static String hexStr2AsciiStr(String hex) {
        String rec = null;
        try {
            rec = new String(ByteUtils.hexString2Bytes(hex), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return rec;
    }

}
