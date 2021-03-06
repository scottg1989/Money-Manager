package sg.money.fragments;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Locale;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import sg.money.domainobjects.Budget;
import sg.money.adapters.BudgetListAdapter;
import sg.money.common.DatabaseManager;
import sg.money.utils.DialogButtons;
import sg.money.utils.Misc;
import sg.money.R;
import sg.money.activities.SettingsActivity;
import sg.money.domainobjects.Transaction;
import sg.money.activities.AddBudgetActivity;

public class BudgetsFragment extends HostActivityFragmentBase implements OnItemLongClickListener, OnItemClickListener
{

	private ListView m_budgetsList;
    private TextView m_txtMonth;
    private ArrayList<Budget> m_budgets;
    private BudgetListAdapter m_adapter;
    private SimpleDateFormat m_monthYearFormat;
    private String m_currentMonth;
    private ArrayList<Transaction> m_transactions;

    private static final int REQUEST_ADDBUDGET = 0;
    private static final int REQUEST_SETTINGS = 1;
	
	//Bundle State Data
	private static final String STATE_MONTH = "stateMonth";


    /* Fragment overrides */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

        View v = inflater.inflate(R.layout.activity_budgets, null);
		
		m_budgetsList = (ListView)v.findViewById(R.id.budgetsList);
        m_txtMonth = (TextView)v.findViewById(R.id.txtMonth);
        
        View emptyView = v.findViewById(android.R.id.empty);
    	((TextView)v.findViewById(R.id.empty_text)).setText("No budgets");
    	((TextView)v.findViewById(R.id.empty_hint)).setText("Use the add button to create one.");
    	m_budgetsList.setEmptyView(emptyView);

        setActionMode(null);
        m_budgetsList.setItemsCanFocus(false);
        m_budgetsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        m_budgetsList.setOnItemClickListener(this);
        m_budgetsList.setOnItemLongClickListener(this);
		
		if (savedInstanceState != null)
    	{
    		m_currentMonth = savedInstanceState.getString(STATE_MONTH);
        	setData(m_currentMonth);
    	}
    	else
    	{
        	setData("");
    	}

        if (savedInstanceState == null)
        {
            getParentActivity().invalidateOptionsMenu();
        }

        return v;
	}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_MONTH, m_currentMonth);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getParentActivity().getSupportMenuInflater().inflate(R.menu.activity_budgets, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_addbudget: {
                Intent intent = new Intent(getParentActivity(), AddBudgetActivity.class);
                startActivityForResult(intent, REQUEST_ADDBUDGET);
                break;
            }

            case R.id.menu_month:{
                createDialog().show();
                break;
            }

            case R.id.menu_settings: {
                startActivityForResult(new Intent(getParentActivity(), SettingsActivity.class), REQUEST_SETTINGS);
                break;
            }
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADDBUDGET: {
                if (resultCode == getParentActivity().RESULT_OK) {
                    setData(m_currentMonth);
                }
                break;
            }
            case REQUEST_SETTINGS:
                setData(m_currentMonth);
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu, boolean drawerIsOpen) {
        menu.findItem(R.id.menu_addbudget).setVisible(!drawerIsOpen);
        menu.findItem(R.id.menu_month).setVisible(!drawerIsOpen);
        return true;
    }


    /* Listener callbacks */

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (getActionMode() == null)
		{
			onListItemClick(parent, view, position, id);
		}
		else
		{
			changeItemCheckState(position, m_budgetsList.isItemChecked(position));
		}
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (getActionMode() == null) {
        	setActionMode(getParentActivity().startActionMode(new ModeCallback()));
        }

		m_budgetsList.setItemChecked(position, !m_budgetsList.isItemChecked(position));
		changeItemCheckState(position, m_budgetsList.isItemChecked(position));
        
		return true;
	}
	
	public void changeItemCheckState(int position, boolean checked) {
        m_adapter.setSelected(position, checked);
        m_adapter.notifyDataSetChanged();
    	final int checkedCount = m_adapter.getSelectedItems().size();
        switch (checkedCount) {
            case 0:
                getActionMode().setSubtitle(null);
                break;
            case 1:
                getActionMode().getMenu().clear();
                getActionMode().setSubtitle("" + checkedCount + " selected");
                getActionMode().getMenuInflater().inflate(R.menu.standard_cab, getActionMode().getMenu());
                break;
            default:
                getActionMode().getMenu().clear();
                getActionMode().getMenuInflater().inflate(R.menu.standard_cab_multiple, getActionMode().getMenu());
                getActionMode().setSubtitle("" + checkedCount + " selected");
                break;
        }
        
        if (m_adapter.getSelectedItems().size() == 0)
            getActionMode().finish();
    }

    protected void onListItemClick(AdapterView<?> l, View v, int position, long id) {
        EditItem(m_budgets.get(position));
    }


    /* Methods */

	private void setData(String month) {
		if (month.equals(""))
    	{ 
    		Calendar currentDate = Calendar.getInstance();
    	    currentDate.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), 1, 0, 0, 1);
    	    
        	month = m_monthYearFormat.format(currentDate.getTime());
    	}
		
		m_txtMonth.setText(month);
    	m_currentMonth = month;
    	
    	Calendar startDate = new GregorianCalendar();
    	try {
			startDate.setTime(m_monthYearFormat.parse(month));
		} catch (ParseException e) {
			e.printStackTrace(); 
		}
    	Calendar endDate = (Calendar)startDate.clone();
    	endDate.add(Calendar.MONTH, 1);
		endDate.add(Calendar.SECOND, -1);
    	
    	m_transactions = DatabaseManager.getInstance(getParentActivity()).GetAllTransactions(startDate.getTime(), endDate.getTime());
		m_budgets = DatabaseManager.getInstance(getParentActivity()).GetAllBudgets();
		m_adapter = new BudgetListAdapter(getParentActivity(), m_budgets, m_transactions);
		m_budgetsList.setAdapter(m_adapter);
	}

	public class DateComparator implements Comparator<Transaction> {
	    public int compare(Transaction o1, Transaction o2) {
	        return o1.getDateTime().compareTo(o2.getDateTime());
	    }
	}
	
	private Dialog createDialog()
	{
		m_transactions = DatabaseManager.getInstance(getParentActivity()).GetAllTransactions();
		Collections.sort(m_transactions, new DateComparator());
    	Collections.reverse(m_transactions);

    	final ArrayList<String> months = new ArrayList<String>();
    	if (m_transactions.size() == 0)
    	{
    		Calendar currentDate = Calendar.getInstance();
		    currentDate.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), 1, 0, 0, 0);
		    months.add(m_monthYearFormat.format(currentDate.getTime()));
    	}
    	else
    	{
	    	Transaction oldestTransaction = m_transactions.get(m_transactions.size()-1);
	    	Calendar oldestDate = Calendar.getInstance();
	    	oldestDate.setTime(oldestTransaction.getDateTime());
	    	oldestDate.set(oldestDate.get(Calendar.YEAR), oldestDate.get(Calendar.MONTH), 1, 0, 0, 0);
		    
		    Calendar currentDate = Calendar.getInstance();
		    currentDate.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), 1, 0, 0, 0);
	    	while(true)
	    	{
	    		months.add(m_monthYearFormat.format(currentDate.getTime()));
	    		currentDate.add(Calendar.MONTH, -1);
	    		if (currentDate.before(oldestDate))
	    			break;
	    	}
    	}
    	
		AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
	    builder.setTitle("Select a month")
	           .setItems(months.toArray(new CharSequence[months.size()]), new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int position) {
	               setData(months.get(position));
	           }
	    }).setNegativeButton("Cancel", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
	    return builder.create();
	}
	
	private void EditItem(Budget selectedItem)
	{			
		Intent intent = new Intent(getParentActivity(), AddBudgetActivity.class);
		intent.putExtra("ID", selectedItem.getId());
		startActivityForResult(intent, REQUEST_ADDBUDGET);
	}
	
	private void confirmDeleteItems(final ActionMode mode)
	{
		Misc.showConfirmationDialog(getParentActivity(),
                m_adapter.getSelectedItems().size() == 1
                        ? "Delete 1 budget?"
                        : "Delete " + m_adapter.getSelectedItems().size() + " budgets?",
                DialogButtons.OkCancel,
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        DeleteItems();
                        mode.finish();
                    }
                },
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mode.finish();
                    }
                }
        );
	}
	
	private void DeleteItems()
	{
		ArrayList<Budget> selectedItems = m_adapter.getSelectedItems();
		for(Budget selectedItem : selectedItems)
		{
			DatabaseManager.getInstance(getParentActivity()).DeleteBudget(selectedItem);
		}
		setData(m_currentMonth);
	}


    /* ModeCallback class */

	private final class ModeCallback implements ActionMode.Callback {
   	 
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create the menu from the xml file
            MenuInflater inflater = getParentActivity().getSupportMenuInflater();
            inflater.inflate(R.menu.standard_cab, menu);
            return true;
        }
 
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
 
        public void onDestroyActionMode(ActionMode mode) {
			m_adapter.clearSelected();
	        m_adapter.notifyDataSetChanged();
	        m_budgetsList.clearChoices();
 
            if (mode == getActionMode()) {
            	setActionMode(null);
            }
        }
 
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.cab_edit:
            	EditItem(m_adapter.getSelectedItems().get(0));
                mode.finish();
                return true;
            case R.id.cab_delete:
            	confirmDeleteItems(mode);
                return true;
            default:
                return false;
        }
        }
    }
}
