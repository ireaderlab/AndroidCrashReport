import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 此类表示一个崩溃日志，一个崩溃日志有两种类型：java或者C。
 * 一个崩溃日志可以是单行的(行内用\\n分隔)，也可以是多行的。
 * 对于一个崩溃日志的字符串，有三种状态:
 * 原始状态：崩溃日志是单行的，日志包含\\n，\\t等转义字符。
 * 中间状态：替换掉原始状态的转义字符。
 * 符号状态：日志包含明确的崩溃函数的名称和源文件行号。
 *         对于c日志，符号状态意味着调用“ndk-stack”命令将中间状态的日志解析之后的结果。
 *         对于java日志，符号状态和中间状态相同，因为java不需要解析符号表。
 */
public class CrashItem {
	/**
	 * 崩溃次数
	 */
	private int mCrashCount = 1;
	/**
	 * 原始的crash字符串
	 */
	private String mOriginCrashString;
	/**
	 * 崩溃中间字符串
	 */
	private String mMidCrashString;
	/**
	 * 崩溃符号字符串
	 */
	private String mSymbolCrashString;
	/**
	 * 崩溃ID
	 */
	private String mCrashId;
	/**
	 * 崩溃关键词
	 */
	private String mKeyword;
	/**
	 * 是否是有效的崩溃日志
	 */
	private boolean mValid;
	/**
	 * 崩溃类型
	 */
	private CrashType mCrashType;
	
	/**
	 *  构造一个崩溃日志
	 * @param crashString 崩溃的原始字符串。
	 * @param isMutiline 崩溃日志是否是多行的,对于多行日志，原始字符串和中间字符串是一样的。
	 */
	public CrashItem(String crashString, boolean isMutiline) {
		mOriginCrashString = crashString;
		if (crashString.startsWith("*** *** ***")) {
			mCrashType = CrashType.C_CRASH;
		} else {
			mCrashType = CrashType.JAVA_CRASH;
		}

		if (mCrashType == CrashType.JAVA_CRASH) {
			crashString = crashString.replaceAll("\\\\t", "\t");
		}
		String separator = isMutiline ? "\\n" : "\\\\n";
		String[] lines = crashString.split(separator);
		mMidCrashString = "";
		for (String line : lines) {
			mMidCrashString += "\t";
			mMidCrashString += line;
			mMidCrashString += "\n";
		}

		if (mCrashType == CrashType.JAVA_CRASH) {
			setJavaSymbolCrashLines(lines);
		}
	}

	/**
	 * 设置java崩溃符号字符串，确定崩溃ID,崩溃关键词.
	 * @param lines 符号字符串
	 */
	private void setJavaSymbolCrashLines(String[] lines) {
		if (lines.length < 2) {
			return;
		}
		if (!lines[1].startsWith("\tat ")) {
			return;
		}
		mCrashId = mMidCrashString;
		mKeyword = lines[1];
		mSymbolCrashString = mMidCrashString;
		mValid = true;
	}

	/**
	 * 设置C崩溃符号字符串，确定崩溃ID,崩溃关键词.
	 * @param crashLines 符号字符串，"ndk-stack"解析后的字符串。
	 */
	public void setCSymbolCrashLines(ArrayList<String> crashLines) {
		if (mCrashType != CrashType.C_CRASH) {
			throw new IllegalStateException();
		}
		if (crashLines.size() < 4) {
			return;
		}

		makeCrashID(crashLines);
		makeCrashKeyword(crashLines);

		mSymbolCrashString = "";
		for (int i = 0; i < crashLines.size(); i++) {
			mSymbolCrashString += "\t";
			mSymbolCrashString += crashLines.get(i);
			mSymbolCrashString += "\n";
		}
		mValid = true;
	}

	/**
	 * @return 返回中间字符串。
	 */
	public String getMidCrashString() {
		return mMidCrashString;
	}

	/**
	 * @return 返回符号字符串。
	 */
	public String getSymbolCrashString() {
		return mSymbolCrashString;
	}

	/**
	 * @return 返回原始字符串。
	 */
	public String getOriginCrashString() {
		return mOriginCrashString;
	}

	/**
	 * @return 日志是否是有效的。
	 */
	public boolean isValid() {
		return mValid;
	}

	/**
	 * 增加崩溃次数。
	 */
	public void addCrashCount() {
		mCrashCount++;
	}

	/**
	 * @return 获得崩溃次数。
	 */
	public int getCrashCount() {
		return mCrashCount;
	}

	/**
	 * @return 返回崩溃ID。
	 */
	public String getCrashId() {
		return mCrashId;
	}

	/**
	 * @return 返回崩溃关键字。
	 */
	public String getKeyword() {
		return mKeyword;
	}

	/**
	 * @return 返回崩溃类型。
	 */
	public CrashType getCrashType() {
		return mCrashType;
	}

	/**
	 * 比较两个崩溃是否相等。
	 * @return 如果崩溃ID一样返回true,否则返回false。
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		CrashItem item = (CrashItem) o;
		return item.mCrashId.equals(mCrashId);
	}

	/**
	 * 分析崩溃日志，创建C崩溃日志的CrashID。
	 * @param crashLines 崩溃符号字符串。
	 */
	private void makeCrashID(ArrayList<String> crashLines) {
		// Stack frame #00 pc 00033960
		// /data/app-lib/com.chaozh.iReaderFree-1/libUiControl.so
		String regex = "Stack\\s+frame\\s+#\\d{2,}\\s+pc\\s+[\\d|a-f|A-F]{8,}\\s+/\\S+";
		Pattern pattern = Pattern.compile(regex);
		mCrashId = "";
		for (int i = 4; i < crashLines.size(); i++) {
			String line = crashLines.get(i);
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				String soPath = line.substring(matcher.start(), matcher.end());
				String soName = FileHelper.getFileName(soPath);
				int index = matcher.start() + soPath.length() - soName.length();
				String lineCrashId = line.substring(index);
				mCrashId += lineCrashId;
			} else {
				mCrashId += line;
			}
			mCrashId += "\n";
		}

	}

	/**
	 * 分析崩溃日志，创建崩溃日志关键词。
	 * @param crashLines 崩溃符号字符串。
	 */
	private void makeCrashKeyword(ArrayList<String> crashLines) {
		// Stack frame #01 pc 0008e159 /data/libUiControl.so (Native Method):
		// Routine MobiInputStream::readContent(void*, unsigned int) at
		// /VMobiInputStream.cpp:147 (discriminator 1)
		String regex = "Routine\\s+(.*)\\s+at\\s*(.+):";
		Pattern pattern = Pattern.compile(regex);

		mKeyword = "";
		for (int i = 4; i < crashLines.size(); i++) {
			String line = crashLines.get(i);
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				String funName = line.substring(matcher.start(1), matcher.end(1));
				String filePath = line.substring(matcher.start(2), matcher.end(2));
				String fileName = FileHelper.getFileName(filePath);
				mKeyword = funName + " " + fileName;
				break;
			}
		}
	}
}