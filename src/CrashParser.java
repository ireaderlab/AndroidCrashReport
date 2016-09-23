import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class CrashParser {
	/**
	 * dump文件路径
	 */
	private String mDumpFilePath;
	/**
	 * SO符号表路径
	 */
	private String mSymbolFilePath; 
	/**
	 * 统计报告中间文件输出路径
	 */
	private String mMiddleFilePath;
	/**
	 * 统计报告输出路径
	 */
	private String mReportFilePath;
	/**
	 * 详细的统计报告输出路径
	 */
	private String mDetailReportFilePath;
	/**
	 * 无效日志路径
	 */
	private String mInvalidFilePath;
	/**
	 * 统计报告中间文件输出流
	 */
	private PrintWriter mMidWriter;
	/**
	 * 统计报告输出流
	 */
	private PrintWriter mReportWriter;
	/**
	 * 详细的统计报告输出流
	 */
	private PrintWriter mDetailReportWriter;
	/**
	 * 暂时禁止输出报告，和mReportWriter配合使用
	 */
	private boolean mDisableReportPrint;
	/**
	 * java崩溃日志的总数量
	 */
	private int mJavaCrashCount;
	/**
	 *  C崩溃日志的总数量
	 */
	private int mCCrashCount;
	/**
	 * 最多从mDumpFilePath文件读取多少条崩溃
	 */
	private int mCrashMax;
	/**
	 * 一个崩溃日志为多行格式时，累加每行到此字符串
	 */
	private String mMutilineCrashString;
	/**
	 * 崩溃组列表
	 */
	private ArrayList<CrashGroup> mCrashGroups = new ArrayList<CrashGroup>(); // 崩溃组列表
	/**
	 * 无效的崩溃日志列表
	 */
	private ArrayList<CrashItem> mInvalidItems = new ArrayList<CrashItem>(); // 无效的崩溃日志列表
	/**
	 * 统计报告是否输出到System.out
	 */
	private boolean mEnableStdOut; // 统计报告是否输出到System.out
	/**
	 * mSymbolFilePath为null，却出现了C崩溃日志
	 */
	private boolean mWarnCCrash; // mSymbolFilePath为null，却出现了C崩溃日志
	/**
	 *  报告输出目录
	 */
	private String mOutputDir; // 报告输出目录
	
	/**
	 * 构建一个崩溃解析器。
	 * @param dumpFilePath dump文件路径。
	 * @param symbolFilePath SO符号表路径。
	 * @param crashMax 最多从mDumpFilePath文件读取多少条崩溃。
	 */
	public CrashParser(String dumpFilePath, String symbolFilePath, int crashMax) {
		mSymbolFilePath = symbolFilePath;
		mDumpFilePath = dumpFilePath;
		mCrashMax = crashMax;
		
		File file = new File(mDumpFilePath);
		String dumpDir = file.getParent();
		String dumpName = file.getName();
		int index = dumpName.lastIndexOf(".");
		if (index != -1) {
			dumpName = dumpName.substring(0, index);
		}
		mOutputDir = new File(dumpDir, dumpName + "_crash_report").getAbsolutePath();
		FileHelper.delete(mOutputDir);
		new File(mOutputDir).mkdirs();
	}

	/**
	 * 解析日志，一条日志有可能是单行的(以\\n分隔)，也有可能是多行的。
	 * 解析器每次读取一行日志，判断日志类型. 
	 * @throws java.io.IOException 如果发生IO异常
	 * @throws java.lang.InterruptedException  如果发生异常
	 */
	public void parseCrash() throws IOException, InterruptedException {
		InputStream fin;
		Reader fReader;
		BufferedReader bReader;
		fin = new FileInputStream(mDumpFilePath);
		String encode = decideFileEncode(mDumpFilePath);
		if (encode == null) {
			encode = "utf-8";
		} else if (encode == "unicode") {
			fin.skip(2);
		} else {
			fin.skip(3);
		}
		fReader = new InputStreamReader(fin, encode);
		bReader = new BufferedReader(fReader);
		String line;
		mMutilineCrashString = "";
		while ((line = bReader.readLine()) != null) {
			if (line.contains("\\n")) {
				finishMutilineCrashString();
				CrashItem crashItem = new CrashItem(line, false);
				addCrash(crashItem);
			} else {
				if (line.startsWith("*** *** ***") || line.isEmpty()) {
					finishMutilineCrashString();
				}
				if (!line.isEmpty()) {
					mMutilineCrashString += line;
					mMutilineCrashString += "\n";
				}
			}
			if (mCrashMax != 0 && mJavaCrashCount + mCCrashCount >= mCrashMax) {
				break;
			}
		}

		if (mCrashMax == 0 || mJavaCrashCount + mCCrashCount < mCrashMax) {
			finishMutilineCrashString();
		}
		bReader.close();

		sortCrash();
		printCrash(CrashType.JAVA_CRASH);
		printCrash(CrashType.C_CRASH);
		printInvalidCrash();

		if (mWarnCCrash) {
			System.out.println("warn: dump file contain c crash,but \"-sym\" param not special.");
		}
	}

	/**
	 * 根据文件头，判断文件编码。
	 * @param filePath 判断文件编码
	 * @return 返回文件编码。
	 */
	private String decideFileEncode(String filePath) throws IOException {
		byte buff[] = new byte[3];
		FileInputStream in = null;
		try {
			in = new FileInputStream(filePath);
			int readBytes = in.read(buff);
			if (readBytes >= 2 && buff[0] == (byte) 0xFF && buff[1] == (byte) 0xFE) {
				return "unicode";
			} else if (readBytes >= 3 && buff[0] == (byte) 0xEF && buff[1] == (byte) 0xBB && buff[2] == (byte) 0xBF) {
				return "utf-8";
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return null;
	}

	/**
	 * 完成多行崩溃。
	 */
	private void finishMutilineCrashString() throws IOException, InterruptedException {
		if (mMutilineCrashString.isEmpty()) {
			return;
		}
		CrashItem crashItem = new CrashItem(mMutilineCrashString, true);
		addCrash(crashItem);
		mMutilineCrashString = "";
	}

	/**
	 * 添加一个崩溃。
	 * @param crashItem 一个CrashItem.
	 */
	private void addCrash(CrashItem crashItem) throws IOException, InterruptedException {
		if (crashItem.getCrashType() == CrashType.JAVA_CRASH) {
			mJavaCrashCount++;
		} else {
			if (mSymbolFilePath == null) {
				mWarnCCrash = true;
				return;
			}
			mCCrashCount++;
		}
		System.out.println(String.format("正在解析第%d个日志...", mJavaCrashCount + mCCrashCount));
		decodeCrash(crashItem);
		if (!crashItem.isValid()) {
			mInvalidItems.add(crashItem);
			return;
		}

		CrashGroup crashGroup = findCrashGroup(crashItem);
		if (crashGroup == null) {
			mCrashGroups.add(new CrashGroup(crashItem));
		} else {
			crashGroup.addCrash(crashItem);
		}
	}

	/**
	 * 找到CrashItem的在的崩溃组
	 * @param item 一个CrashItem.
	 * @return 如果找到，返回组，否则，返回null。
	 */
	private CrashGroup findCrashGroup(CrashItem item) {
		for (int i = 0; i < mCrashGroups.size(); i++) {
			if (mCrashGroups.get(i).matchCrash(item)) {
				return mCrashGroups.get(i);
			}
		}
		return null;
	}

	/**
	 * 排序崩溃。
	 */
	private void sortCrash() {
		Collections.sort(mCrashGroups,new Comparator<CrashGroup>() {
			@Override
			public int compare(CrashGroup o1, CrashGroup o2) {
				int o1Count = o1.getCrashCount();
				int o2Count = o2.getCrashCount();
				if (o1.getKeyword().isEmpty()) {
					o1Count = 0;
				}
				if (o2.getKeyword().isEmpty()) {
					o2Count = 0;
				}
				return o2Count - o1Count;
			}
		});

		for (int i = 0; i < mCrashGroups.size(); i++) {
			CrashGroup group = mCrashGroups.get(i);
			group.sort();
		}
	}

	/**
	 * 调用ndk-stack，解析崩溃日志。
	 */
	private void decodeCrash(CrashItem item) throws IOException, InterruptedException {
		if (item.getCrashType() != CrashType.C_CRASH) {
			return;
		}
		String command = String.format("ndk-stack -sym %s", mSymbolFilePath);
		Process proc = Runtime.getRuntime().exec(command);
		proc.getOutputStream().write(item.getMidCrashString().getBytes("utf-8"));
		proc.getOutputStream().close();

		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "utf-8"));
		String line = null;
		ArrayList<String> crashLines = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			crashLines.add(line);
		}
		item.setCSymbolCrashLines(crashLines);
	}

	/**
	 * 输出信息。
	 */
	private void printMessage(String msg) {
		if (mMidWriter != null) {
			mMidWriter.println(msg);
		}
		if (mReportWriter != null) {
			if (!mDisableReportPrint) {
				mReportWriter.println(msg);
			}
		}
		if (mDetailReportWriter != null) {
			mDetailReportWriter.println(msg);
		}
		if (mEnableStdOut) {
			System.out.println(msg);
		}
	}

	/**
	 * 创建崩溃日志输出流。
	 */
	private void createPrintStream(CrashType crashType) throws IOException {
		String des = crashType == CrashType.JAVA_CRASH ? "java" : "c";
		// mMiddleFilePath= new File(mOutputDir, des +
		// "_mid.txt").getAbsolutePath();
		mReportFilePath = new File(mOutputDir, des + "_report.txt").getAbsolutePath();
		mDetailReportFilePath = new File(mOutputDir, des + "_detail_report.txt").getAbsolutePath();
		if (mMiddleFilePath != null) {
			mMidWriter = new PrintWriter(new FileWriter(mMiddleFilePath));
		}
		if (mReportFilePath != null) {
			mReportWriter = new PrintWriter(new FileWriter(mReportFilePath));
		}
		if (mDetailReportFilePath != null) {
			mDetailReportWriter = new PrintWriter(new FileWriter(mDetailReportFilePath));
		}
	}

	/**
	 * 输出崩溃报表。
	 */
	private void printCrash(CrashType crashType) throws IOException {
		ArrayList<CrashGroup> crashGroups = new ArrayList<>();
		for (int i = 0; i < mCrashGroups.size(); i++) {
			CrashGroup crashGroup = mCrashGroups.get(i);
			if (crashGroup.getCrashType() == crashType) {
				crashGroups.add(crashGroup);
			}
		}

		if (crashGroups.isEmpty()) {
			return;
		}

		createPrintStream(crashType);
		String carshTypeDes = crashType == CrashType.JAVA_CRASH ? "java" : "C";
		int crashCount = (crashType == CrashType.JAVA_CRASH ? mJavaCrashCount : mCCrashCount);
		String msg = String.format("本次共分析%d个%s日志，分析后得到%d种崩溃", crashCount, carshTypeDes, crashGroups.size());
		printMessage(msg);
		printMessage("");

		for (int i = 0; i < crashGroups.size(); i++) {
			CrashGroup group = crashGroups.get(i);
			msg = String.format(
					"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<开始崩溃组<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			printMessage(msg);
			msg = String.format("崩溃编号:%d", i + 1);
			printMessage(msg);
			msg = String.format("崩溃触发函数:%s", group.getKeyword());
			printMessage(msg);
			msg = String.format("崩溃总次数:%d", group.getCrashCount());
			printMessage(msg);
			printMessage("崩溃关键函数:");
			printMessage("崩溃分析:");
			msg = ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>";
			printMessage(msg);
			for (int j = 0; j < group.size(); j++) {
				if (j >= 2) {
					mDisableReportPrint = true;
				}
				CrashItem crashItem = group.get(j);

				msg = String.format("\t崩溃次数:%d", crashItem.getCrashCount());
				printMessage(msg);
				if (mMidWriter != null) {
					mMidWriter.println(crashItem.getMidCrashString());
				}
				if (mReportWriter != null) {
					if (j < 2) {
						mReportWriter.println(crashItem.getSymbolCrashString());
					}
				}
				if (mDetailReportWriter != null) {
					mDetailReportWriter.println(crashItem.getSymbolCrashString());
				}
				if (mEnableStdOut) {
					System.out.println(crashItem.getSymbolCrashString());
				}

				mDisableReportPrint = false;
			}
			if (mReportWriter != null) {
				if (group.size() > 2) {
					mReportWriter.print("\t...");
				}
			}
			printMessage("\n\n");
		}
		if (mMidWriter != null) {
			mMidWriter.close();
		}
		if (mReportWriter != null) {
			mReportWriter.close();
		}
		if (mDetailReportWriter != null) {
			mDetailReportWriter.close();
		}
	}

	/**
	 * 输出无效的日志。
	 */
	private void printInvalidCrash() throws IOException {
		if (mInvalidItems.size() == 0) {
			return;
		}
		mInvalidFilePath = new File(mOutputDir, "invalid.txt").getAbsolutePath();
		PrintWriter invalidWriter = new PrintWriter(new FileWriter(mInvalidFilePath));
		invalidWriter.println(String.format("共有%d个无效日志.\n", mInvalidItems.size()));
		for (int i = 0; i < mInvalidItems.size(); i++) {
			CrashItem crashItem = mInvalidItems.get(i);
			invalidWriter.println(crashItem.getOriginCrashString());
		}
		invalidWriter.close();
	}
}