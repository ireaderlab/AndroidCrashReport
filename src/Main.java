import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		// 分析命令行参数
		ArgParser argParser = new ArgParser();
		if (!argParser.parse(args)) {
			return;
		}
		
		// 解析日志，输出报表。
		CrashParser crashParser = new CrashParser(argParser.getDumpParam(), argParser.getSymParam(),
				argParser.getMaxParam());
		long st = System.currentTimeMillis();
		crashParser.parseCrash();
		long et = System.currentTimeMillis();
		long min = (et - st) / 1000 / 60;
		long sec = (et - st) / 1000 % 60;
		System.out.println(String.format("解析日志完成，共用时:%d分%d秒", min, sec));
	}
}
