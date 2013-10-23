package sg.money.fragments;

/**
 * The concrete types of fragments which may be accessed via the navigation drawer
 */
public enum HostActivityFragmentTypes {
    Transactions(0),
    Accounts(1),
    Categories(2),
    Overview(3),
    Budgets(4);

	// access to values() for casting is expensive, so use this instead..
	public static HostActivityFragmentTypes fromInteger(int x) {
		switch(x) {
			case 0:
				return Transactions;
			case 1:
				return Accounts;
			case 2:
				return Categories;
			case 3:
				return Overview;
			case 4:
				return Budgets;
		}
		return null;
	}

	private final int value;
	private HostActivityFragmentTypes(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
