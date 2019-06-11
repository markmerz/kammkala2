package ee.ut.mrz.kammkala.web;

import org.zkoss.zul.ListModel;
import org.zkoss.zul.event.ListDataListener;


public class RoutersConfigListModel implements ListModel {

	@Override
	public Object getElementAt(int index) {
		try {
			return DataBase.getRoutersCached().get(index);
		} catch (Exception e) {
			return null;
		}		
	}

	@Override
	public int getSize() {
		try {
			return DataBase.getRoutersCached().size();
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		// TODO Auto-generated method stub

	}

}
