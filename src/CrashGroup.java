import java.util.ArrayList;
import java.util.Comparator;

/**
 * 此类表示一个崩溃组，代表一类崩溃。一个崩溃组里面的崩溃具有相同的崩溃关键字和崩溃类型。
 */
public class CrashGroup {
	/**
	 * 崩溃列表
	 */
	private ArrayList<CrashItem> mCrashItems = new ArrayList<CrashItem>();

	/**
	 * 构造一个崩溃组。
	 * @param item 第一个CrashItem，不能为null。
	 */
	public CrashGroup(CrashItem item) {
		mCrashItems.add(item);
	}

	/**
	 * 获得崩溃的数量。
	 * @return 返回崩溃组里面的CrashItem的数量。
	 */
	public int size() {
		return mCrashItems.size();
	}

	/**
	 * 获得一个崩溃。
	 * @param index 索引。
	 * @return 返回一个CrashItem
	 */
	public CrashItem get(int index) {
		return mCrashItems.get(index);
	}

	/**
	 * crashItem是否可以添加到该组。
	 * @param item 要匹配的CrashItem。
	 * @return 如果item能添加到组内，返回true,否则返回false.
	 */
	public boolean matchCrash(CrashItem item) {
		if (!item.getKeyword().equals(getKeyword())) {
			return false;
		}
		if (item.getCrashType() != getCrashType()) {
			return false;
		}
		return true;
	}
	
	/**
	 * 添加一个CrashItem。
	 * @param item 一个Crashitem。
	 * @return 成功添加返回true，否则返回false。
	 */
	public boolean addCrash(CrashItem item) {
		if (!matchCrash(item)) {
			return false;
		}
		CrashItem findItem = findSameCrash(item);
		if (findItem != null) {
			findItem.addCrashCount();
		} else {
			mCrashItems.add(item);
		}
		return true;
	}

	/**
	 * 在组内寻找一个相同的CrashItem。
	 * @param item 要寻找的CrashItem。
	 * @return 如果找到，返回该对象，否则返回null。
	 */
	public CrashItem findSameCrash(CrashItem item) {
		for (int i = 0; i < mCrashItems.size(); i++) {
			if (mCrashItems.get(i).equals(item)) {
				return mCrashItems.get(i);
			}
		}
		return null;
	}

	/**
	 * @return 返回崩溃关键词。
	 */
	public String getKeyword() {
		return mCrashItems.get(0).getKeyword();
	}

	/**
	 * @return 返回崩溃类型。
	 */
	public CrashType getCrashType() {
		return mCrashItems.get(0).getCrashType();
	}

	/**
	 * @return 返回所有的CrashItem的崩溃次数累加之和。
	 */
	public int getCrashCount() {
		int count = 0;
		for (int i = 0; i < mCrashItems.size(); i++) {
			count += mCrashItems.get(i).getCrashCount();
		}
		return count;
	}

	/**
	 * 按照崩溃次数降序排列。
	 */
	public void sort() {
		mCrashItems.sort(new Comparator<CrashItem>() {
			@Override
			public int compare(CrashItem o1, CrashItem o2) {
				return o2.getCrashCount() - o1.getCrashCount();
			}

		});
	}
}