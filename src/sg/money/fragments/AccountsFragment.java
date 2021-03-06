package sg.money.fragments;

import java.util.ArrayList;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.Activity;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import sg.money.domainobjects.Account;
import sg.money.adapters.AccountListAdapter;
import sg.money.common.DatabaseManager;
import sg.money.utils.DialogButtons;
import sg.money.utils.Misc;
import sg.money.R;
import sg.money.utils.Settings;
import sg.money.activities.SettingsActivity;
import sg.money.activities.AddAccountActivity;

public class AccountsFragment extends HostActivityFragmentBase implements OnItemLongClickListener, OnItemClickListener
{
	private ListView m_accountsList;
    private ArrayList<Account> m_accounts;
    private AccountListAdapter m_adapter;

    private static final int REQUEST_ADDACCOUNT = 0;
    private static final int REQUEST_SETTINGS = 10;


    /* Fragment overrides */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        View v = inflater.inflate(R.layout.activity_accounts, null);

        m_accountsList = (ListView)v.findViewById(R.id.accountsList);

        View emptyView = v.findViewById(android.R.id.empty);
        ((TextView)v.findViewById(R.id.empty_text)).setText("No accounts");
        ((TextView)v.findViewById(R.id.empty_hint)).setText("Use the add button to create one.");
        m_accountsList.setEmptyView(emptyView);

        setActionMode(null);
        m_accountsList.setItemsCanFocus(false);
        m_accountsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        m_accountsList.setOnItemClickListener(this);
        m_accountsList.setOnItemLongClickListener(this);

        UpdateList();

        if (savedInstanceState == null)
        {
            getParentActivity().invalidateOptionsMenu();
        }

        return v;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getParentActivity().getSupportMenuInflater().inflate(R.menu.activity_accounts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.menu_addaccount:{
                Intent intent = new Intent(getParentActivity(), AddAccountActivity.class);
                startActivityForResult(intent, REQUEST_ADDACCOUNT);
                break;
            }

            case R.id.menu_settings:{
                startActivityForResult(new Intent(getParentActivity(), SettingsActivity.class), REQUEST_SETTINGS);
                break;
            }
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case REQUEST_ADDACCOUNT:
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    UpdateList();
                }
                break;
            }
            case REQUEST_SETTINGS:
            {
                UpdateList();
                break;
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu, boolean drawerIsOpen) {
        menu.findItem(R.id.menu_addaccount).setVisible(!drawerIsOpen);
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
			changeItemCheckState(position, m_accountsList.isItemChecked(position));
		}
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (getActionMode() == null) {
        	setActionMode(getParentActivity().startActionMode(new ModeCallback()));
        }

		m_accountsList.setItemChecked(position, !m_accountsList.isItemChecked(position));
		changeItemCheckState(position, m_accountsList.isItemChecked(position));
        
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


    protected void onListItemClick(AdapterView<?> l, View v, int position, long id)
    {
        Account account = m_accounts.get(position);
        Settings.setDefaultAccount(getActivity(), account.getId());

        getParentActivity().changeContent(HostActivityFragmentTypes.Transactions);
    }


    /* Methods */
    
    private void UpdateList()
    {
    	m_accounts = DatabaseManager.getInstance(getParentActivity()).GetAllAccounts();
 
		m_adapter = new AccountListAdapter(getParentActivity(), m_accounts);
		m_accountsList.setAdapter(m_adapter);
    }
	
	private void EditItem()
	{
		Account selectedItem = m_adapter.getSelectedItems().get(0);
		Intent intent = new Intent(getParentActivity(), AddAccountActivity.class);
		intent.putExtra("ID", selectedItem.getId());
    	startActivityForResult(intent, REQUEST_ADDACCOUNT);
	}
	
	private void confirmDeleteItems(final ActionMode mode)
	{
		Misc.showConfirmationDialog(getParentActivity(),
                m_adapter.getSelectedItems().size() == 1
                        ? "Delete 1 account?"
                        : "Delete " + m_adapter.getSelectedItems().size() + " accounts?",
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
		ArrayList<Account> selectedItems = m_adapter.getSelectedItems();
		for(Account selectedItem : selectedItems)
		{
			DatabaseManager.getInstance(getParentActivity()).DeleteAccount(selectedItem);
		}
		UpdateList();
	}


    /* ModeCallback class */

    private final class ModeCallback implements ActionMode.Callback {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
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
            m_accountsList.clearChoices();

            if (mode == getActionMode()) {
                setActionMode(null);
            }
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.cab_edit:
                    EditItem();
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
