package ee.ut.mrz.kammkala.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;


public class SwitchesMVC extends GenericForwardComposer {

	private static final long serialVersionUID = -4218000493583345492L;

	private Textbox switchAddress;
	private Textbox switchCommunity;
	private Button newSwitchSave;
	private Grid sgrid;
	private Window swin;
	private Div newswin;

	public final void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		swin.setAttribute("smvci", this);
		this.onTimer$stimer(null);
	}

	public void onTimer$stimer(Event e) {

		try {
			List<String[]> switchinfos = DataBase.getSwitchesCached();
			Iterator<String[]> si = switchinfos.iterator();
			while (si.hasNext()) {
				String[] switchinfo = si.next();
				String lid = "sr-" + switchinfo[DataBase.SWITCH_ID];
				Label sl = (Label) swin.getFellowIfAny(lid);
				if (sl != null) {
					if (!sl.getValue().equals(
							switchinfo[DataBase.SWITCH_STATUSSTRING])) {
						sl.setValue(switchinfo[DataBase.SWITCH_STATUSSTRING]);
					}
				}

				lid = "sname-" + switchinfo[DataBase.SWITCH_ID];
				sl = (Label) swin.getFellowIfAny(lid);
				if (sl != null) {
					if (!sl.getValue().equals(switchinfo[DataBase.SWITCH_NAME])) {
						sl.setValue(switchinfo[DataBase.SWITCH_NAME]);
					}
				}

				lid = "sup-" + switchinfo[DataBase.SWITCH_ID];
				sl = (Label) swin.getFellowIfAny(lid);
				if (sl != null) {
					if (!sl.getValue().equals(
							switchinfo[DataBase.SWITCH_LASTUPDATE])) {
						sl.setValue(switchinfo[DataBase.SWITCH_LASTUPDATE]);
					}
				}
			}

		} catch (Exception e3) {
			try {
				Messagebox.show(e3.getMessage(), null, Messagebox.OK,
						Messagebox.ERROR);
			} catch (InterruptedException e2) {
			}
		}

	}

	

	public void onClick$newSwitchSave(Event e) {
		String sswitchAddress = switchAddress.getValue();
		String sswitchCommunity = switchCommunity.getValue();

		try {
			DataBase.addSwitch(sswitchAddress, sswitchCommunity);
		} catch (Exception e1) {
			try {
				Messagebox.show(e1.getMessage(), null, Messagebox.OK,
						Messagebox.ERROR);
			} catch (InterruptedException e2) {
			}
		}

		switchAddress.setValue(null);
		switchCommunity.setValue(null);

		sgrid.setModel(sgrid.getModel());

		newswin.setVisible(false);
		

	}

	public void onSwitchDel(Event e) {
		Button b = (Button) e.getTarget();
		int rrhashcode = Integer.parseInt(b.getId().replace("srb-", ""));
		try {
			Messagebox.show("Delete switch?", "Confirm", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new SwitchDelEventListener(rrhashcode, sgrid));			
		} catch (Exception e1) {
			try {
				Messagebox.show(e1.getMessage(), null, Messagebox.OK,
						Messagebox.ERROR);
			} catch (InterruptedException e2) {
			}
		}

	}

}

class SwitchDelEventListener implements EventListener {
	private int switchId;
	private Grid sgrid;
	public SwitchDelEventListener(int switchId, Grid sgrid) {
		super();
		this.switchId = switchId;
		this.sgrid = sgrid;
	}
	@Override
	public void onEvent(Event event) throws Exception {		
		if ((Integer) event.getData() == Messagebox.YES) {
			DataBase.delSwitch(switchId);
			sgrid.setModel(sgrid.getModel());
		}
		
	}
	
}
