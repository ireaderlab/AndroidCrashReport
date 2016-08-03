import java.io.File;
import java.io.IOException;
/**
 * 此类用来处理命令行参数。
 */
public class ArgParser {
	/**
	 * SO符号表的路径
	 */
	private String mSymParam;
	/**
	 * 崩溃日志路径
	 */
	private String mDumpParam;
	/**
	 * 最多分析多少条日志
	 */
	private int mMaxParam;
	
	/**
	 * 解析命令行参数,并验证命令行参数。
	 * @param args 命令行参数
	 * @return 成功返回true,否则返回false
	 */
	public boolean parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-sym")) {
				if (i + 1 < args.length) {
					mSymParam = args[i + 1];
					i++;
				} else {
					printLoseValue(args[i]);
					return false;
				}
			} else if (args[i].equals("-dump")) {
				if (i + 1 < args.length) {
					mDumpParam = args[i + 1];
					i++;
				} else {
					printLoseValue(args[i]);
					return false;
				}
			} else if (args[i].equals("-max")) {
				if (i + 1 < args.length) {
					try {
						mMaxParam = Integer.parseInt(args[i + 1]);
					} catch (NumberFormatException e) {
						System.err.println("error: invalid value for \"max\" parameter.");
						return false;
					}
					i++;
				} else {
					printLoseValue(args[i]);
					return false;
				}
			} else {
				System.err.println(String.format("error: invalid parameter \"%s\",please see usage bellow:", args[i]));
				printUsage();
				return false;
			}
		}

		if (mDumpParam == null) {
			System.err.println("error: lose parameter \"-dump\".");
			printUsage();
			return false;
		}

		if (mSymParam != null) {
			try {
				Runtime.getRuntime().exec("ndk-stack");
			} catch (IOException e) {
				System.err
						.println("error:\"ndk-stack\" command not found,please add \"ndk-stack\" to path environment.");
				return false;
			}
		}
		
		if (mSymParam != null) {
			if (!new File(mSymParam).exists()) {
				System.err.println(String.format("error: can not find file:%s", mSymParam));
				printUsage();
				return false;
			}
		}
		
		if (mDumpParam != null) {
			if (!new File(mDumpParam).exists()) {
				System.err.println(String.format("error: can not find file:%s", mDumpParam));
				printUsage();
				return false;
			}
		}
		return true;
	}

	/**
	 * 某个参数未指定值时，输出提示信息
	 */
	private void printLoseValue(String param) {
		System.err.println(String.format("error: lose value for \"%s\" paramter,please see usage bellow:", param));
		printUsage();
	}

	/**
	 * 输出帮助文档
	 */
	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("   acrash-report -sym <path> [-dump <path>]");
		System.out.println();
		System.out.println("      -dump Contains full path to the file containing the crash dump.");
		System.out.println("      -sym  Contains full path to the root directory for symbols.");
		System.out.println("      This is an optional parameter. If ommited, C crash will ignore.");
	}

	/**
	 * @return 返回SO符号表的路径
	 */
	public String getSymParam() {
		return mSymParam;
	}

	/**
	 * @return 返回崩溃日志路径
	 */
	public String getDumpParam() {
		return mDumpParam;
	}

	/**
	 * @return 返回最多分析多少条日志
	 */
	public int getMaxParam() {
		return mMaxParam;
	}
}
