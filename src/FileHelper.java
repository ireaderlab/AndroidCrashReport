import java.io.File;
import java.util.Stack;
/**
 * 此类用来帮助操作文件
 */
public class FileHelper {
	/**
	 * 删除一个文件，文件夹（文件夹可以非空)。
	 * @param filePath 文件路径。
	 */
	public static void delete(String filePath) {
		Stack<File> fileStack = new Stack<>();
		fileStack.push(new File(filePath));
		do {
			File file = fileStack.peek();
			if (file.isFile()) {
				file.delete();
				fileStack.pop();
			} else if (file.isDirectory()) {
				File [] subFiles = file.listFiles();
				if (subFiles == null || subFiles.length == 0) {
					file.delete();
					fileStack.pop();
				} else {
					for (File sFile : subFiles) {
						fileStack.push(sFile);
					}
				}
			} else {
				fileStack.pop();
			}
		} while(!fileStack.empty());
	}
	
	/**
	 * 获得一个文件路径的文件名。
	 * @param path 文件路径。
	 * @return 返回文件名。
	 */
	public static String getFileName(String path) {
		String[] parts = path.split("/");
		return parts[parts.length - 1];
	}
}
