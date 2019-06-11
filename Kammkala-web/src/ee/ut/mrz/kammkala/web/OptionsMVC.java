package ee.ut.mrz.kammkala.web;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Messagebox;

import ee.ut.mrz.kammkala.config.Config;

public class OptionsMVC extends GenericForwardComposer {

	private static final long serialVersionUID = -2818621761062332923L;

	private Intbox switchpollinterval;
	private Intbox routerpollinterval;
	private Intbox delaybetweenthreads;

	public final void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		setValues();
	}

	public void onClick$save(Event e) {
		Integer switchpollintervali = switchpollinterval.getValue();
		if (switchpollintervali != null) {
			Config.getInstance().setVariable("switchpollinterval",
					switchpollintervali.toString());			
			
		}
		
		Integer routerpollintervali = routerpollinterval.getValue();
		if (routerpollintervali != null) {
			Config.getInstance().setVariable("routerpollinterval",
					routerpollintervali.toString());
		}

		Integer delaybetweenthreadsi = delaybetweenthreads.getValue();
		if (delaybetweenthreadsi != null) {
			Config.getInstance().setVariable("delaybetweenthreads",
					delaybetweenthreadsi.toString());
		}
					
		try {
			Config.getInstance().saveConfig();
			Messagebox.show("Options saved.");
		} catch (Exception e1) {
			try {
				Messagebox.show(e1.getMessage(), null, Messagebox.OK, Messagebox.ERROR);				
			} catch (InterruptedException e2) {				
			}
		}
		
		setValues();
	}

	public void onClick$cancel(Event e) {
		setValues();
	}

	private void setValues() {
		switchpollinterval.setValue(Config.getInstance()
				.getSwitchpollinterval());
		routerpollinterval.setValue(Config.getInstance()
				.getRouterpollinterval());
		delaybetweenthreads.setValue(Config.getInstance()
				.getDelaybetweenthreads());
	}

}
