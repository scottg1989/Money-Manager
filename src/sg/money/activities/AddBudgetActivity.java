package sg.money.activities;

import java.util.ArrayList;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import sg.money.domainobjects.Account;
import sg.money.domainobjects.Budget;
import sg.money.domainobjects.Category;
import sg.money.DatabaseManager;
import sg.money.fragments.HostActivityFragmentTypes;
import sg.money.models.AddBudgetModel;
import sg.money.utils.Misc;
import sg.money.R;

public class AddBudgetActivity extends BaseActivity {
	ArrayList<Budget> currentBudgets;
	ArrayList<Category> currentCategories;
	ArrayList<Account> currentAccounts;

	ArrayList<Category> selectedCategories;
	ArrayList<Account> selectedAccounts;

	ArrayList<String> notifyTypeOptions;

	EditText txtName;
	EditText txtValue;
	Button btnCategories;
	Button btnAccounts;
	Spinner spnNotifyType;

	Budget editBudget;

	// todo change these - just horrible!
	int viewingDialog = 0;
	static final int ACCOUNTSLIST = 1;
	static final int CATEGORIESLIST = 2;

	// Bundle State Data
	static final String STATE_SELECTED_ACCOUNTS = "stateSelectedAccounts";
	static final String STATE_SELECTED_CATEGORIES = "stateSelectedCategories";

    AddBudgetModel model;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_budget);
		setTitle("Add Budget");

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		txtName = (EditText) findViewById(R.id.txtName);
		txtValue = (EditText) findViewById(R.id.txtValue);
		btnCategories = (Button) findViewById(R.id.btnCategories);
		btnAccounts = (Button) findViewById(R.id.btnAccounts);
		spnNotifyType = (Spinner) findViewById(R.id.spnNotifyType);

		btnCategories.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CategoriesClicked();
			}
		});

		btnAccounts.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AccountsClicked();
			}
		});

		currentBudgets = DatabaseManager.getInstance(this).GetAllBudgets();
		currentCategories = new ArrayList<Category>();
		ArrayList<Category> allCategories = DatabaseManager.getInstance(this)
				.GetAllCategories();
		allCategories = Misc.getCategoriesInGroupOrder(allCategories);
		for (Category category : allCategories) {
			if (!category.income)
				currentCategories.add(category);
		}
		currentAccounts = DatabaseManager.getInstance(this).GetAllAccounts();
		selectedCategories = new ArrayList<Category>();
		selectedAccounts = new ArrayList<Account>();

		notifyTypeOptions = new ArrayList<String>();
		notifyTypeOptions.add("None");
		notifyTypeOptions.add("Daily");
		notifyTypeOptions.add("Weekly");
		notifyTypeOptions.add("Monthly");

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
				notifyTypeOptions);
		spnNotifyType.setAdapter(arrayAdapter);

		// check if we are editing
		editBudget = null;
		int editId = getIntent().getIntExtra("ID", -1);
		if (editId != -1) {
			editBudget = DatabaseManager.getInstance(AddBudgetActivity.this)
					.GetBudget(editId);
			txtName.setText(editBudget.name);
			txtValue.setText(String.valueOf(editBudget.value));
			selectedCategories = editBudget.categories;
			selectedAccounts = editBudget.accounts;

			spnNotifyType.setSelection(editBudget.notifyType);

			setTitle("Edit Budget");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		ArrayList<Integer> selectedAccountIds = new ArrayList<Integer>();
		for (Account account : selectedAccounts)
			selectedAccountIds.add(account.id);
		ArrayList<Integer> selectedCategoryIds = new ArrayList<Integer>();
		for (Category category : selectedCategories)
			selectedCategoryIds.add(category.id);

		savedInstanceState.putIntegerArrayList(STATE_SELECTED_ACCOUNTS,
				selectedAccountIds);
		savedInstanceState.putIntegerArrayList(STATE_SELECTED_CATEGORIES,
				selectedCategoryIds);

		super.onSaveInstanceState(savedInstanceState);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		ArrayList<Integer> selectedAccountIds = savedInstanceState
				.getIntegerArrayList(STATE_SELECTED_ACCOUNTS);
		ArrayList<Integer> selectedCategoryIds = savedInstanceState
				.getIntegerArrayList(STATE_SELECTED_CATEGORIES);

		selectedAccounts.clear();
		for (int accountId : selectedAccountIds) {
			for (Account account : currentAccounts) {
				if (account.id == accountId) {
					selectedAccounts.add(account);
					break;
				}
			}
		}

		selectedCategories.clear();
		for (int categoryId : selectedCategoryIds) {
			for (Category category : currentCategories) {
				if (category.id == categoryId) {
					selectedCategories.add(category);
					break;
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_add_budget, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_acceptbudget: {
			OkClicked();
			break;
		}

		case R.id.menu_rejectbudget: {
			CancelClicked();
			break;
		}

		case R.id.menu_settings: {
			break;
		}

		case android.R.id.home:
            Intent intent = new Intent(this, ParentActivity.class);
            intent.putExtra(ParentActivity.INTENTEXTRA_CONTENTTYPE, HostActivityFragmentTypes.Budgets);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
		}
		return true;
	}

	private void CategoriesClicked() {
		boolean[] checkedItems = new boolean[currentCategories.size() + 1];
		checkedItems[0] = selectedCategories.size() == 0;
		for (int i = 0; i < currentCategories.size(); i++) {
			boolean alreadySelected = false;
			for (Category selectedCategory : selectedCategories) {
				if (selectedCategory.id == currentCategories.get(i).id) {
					alreadySelected = true;
					break;
				}
			}
			checkedItems[i + 1] = alreadySelected;
		}

		ArrayList<String> items = new ArrayList<String>();
		items.add("All Categories");
		for (Category category : currentCategories) {
			if (!category.useInReports)
				continue;

			items.add(Misc.getCategoryName(category, currentCategories));
		}

		viewingDialog = CATEGORIESLIST;
		createDialog("Categories", items, checkedItems).show();
	}

	private void AccountsClicked() {
		boolean[] checkedItems = new boolean[currentAccounts.size() + 1];
		checkedItems[0] = selectedAccounts.size() == 0;
		for (int i = 0; i < currentAccounts.size(); i++) {
			boolean alreadySelected = false;
			for (Account selectedAccount : selectedAccounts) {
				if (selectedAccount.id == currentAccounts.get(i).id) {
					alreadySelected = true;
					break;
				}
			}
			checkedItems[i + 1] = alreadySelected;
		}

		ArrayList<String> items = new ArrayList<String>();
		items.add("All Accounts");
		for (Account account : currentAccounts)
			items.add(account.name);

		viewingDialog = ACCOUNTSLIST;
		createDialog("Accounts", items, checkedItems).show();
	}

	private Dialog createDialog(String title, final ArrayList<String> items,
			final boolean[] checkedItems) {
		final ArrayList<Object> mSelectedItems = new ArrayList<Object>(); // Where
																			// we
																			// track
																			// the
																			// selected
																			// items
		for (int i = 1; i < checkedItems.length; i++) {
			if (checkedItems[i] == true)
				mSelectedItems.add(i);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(
				AddBudgetActivity.this);
		// Set the dialog title
		builder.setTitle(title)
				// Specify the list array, the items to be selected by default
				// (null for none),
				// and the listener through which to receive callbacks when
				// items are selected
				.setMultiChoiceItems(
						items.toArray(new CharSequence[items.size()]),
						checkedItems,
						new DialogInterface.OnMultiChoiceClickListener() {
							public void onClick(DialogInterface dialog,
									int which, boolean isChecked) {
								if (isChecked) {
									if (which == 0) {
										mSelectedItems.clear();
										for (int i = 1; i < items.size(); i++) {
											checkedItems[i] = false;
											((AlertDialog) dialog)
													.getListView()
													.setItemChecked(i, false);
										}
										((BaseAdapter) ((AlertDialog) dialog)
												.getListView().getAdapter())
												.notifyDataSetChanged();
									} else {
										mSelectedItems.add(which);
										if (checkedItems[0] == true) {
											checkedItems[0] = false;
											((AlertDialog) dialog)
													.getListView()
													.setItemChecked(0, false);
											((BaseAdapter) ((AlertDialog) dialog)
													.getListView().getAdapter())
													.notifyDataSetChanged();
										}
									}
								} else if (which == 0) {
									if (mSelectedItems.size() == 0) {
										checkedItems[0] = true;
										((AlertDialog) dialog).getListView()
												.setItemChecked(0, true);
										((BaseAdapter) ((AlertDialog) dialog)
												.getListView().getAdapter())
												.notifyDataSetChanged();
									}
								} else if (mSelectedItems.contains(which)) {
									mSelectedItems.remove(Integer
											.valueOf(which));
									if (mSelectedItems.size() == 0) {
										checkedItems[0] = true;
										((AlertDialog) dialog).getListView()
												.setItemChecked(0, true);
										((BaseAdapter) ((AlertDialog) dialog)
												.getListView().getAdapter())
												.notifyDataSetChanged();
									}
								}
							}
						})
				// Set the action buttons
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (viewingDialog == ACCOUNTSLIST)
							selectedAccounts.clear();
						else if (viewingDialog == CATEGORIESLIST)
							selectedCategories.clear();

						for (Object item : mSelectedItems) {
							if (viewingDialog == ACCOUNTSLIST) {
								for (Account account : currentAccounts) {
									if (account.name.equals(items.get(Integer
											.parseInt(String.valueOf(item))))) {
										selectedAccounts.add(account);
										break;
									}
								}
							} else if (viewingDialog == CATEGORIESLIST) {
								for (Category category : currentCategories) {
									if (Misc.getCategoryName(category, currentCategories).equals(items.get(Integer
											.parseInt(String.valueOf(item))))) {
										selectedCategories.add(category);
										break;
									}
								}
							}
						}

						dialog.cancel();
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

		return builder.create();
	}

    public void cancelFocus()
    {
        txtName.clearFocus();
        txtValue.clearFocus();
    }


	private void OkClicked() {

        cancelFocus();

        String validationError = model.validate(this);
        if (validationError != null)
        {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
        }
        else
        {
            model.commit(this);

            setResult(RESULT_OK, new Intent());
            finish();
        }

		//editBudget.notifyType = spnNotifyType.getSelectedItemPosition();
	}

	private void CancelClicked() {
		setResult(RESULT_CANCELED, new Intent());
		finish();
	}
}
