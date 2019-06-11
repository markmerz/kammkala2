package ee.ut.mrz.kammkala.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;


public class RoutersMVC extends GenericForwardComposer {

	private static final long serialVersionUID = -4218000493583345492L;

	private Textbox routerAddress;
	private Textbox routerCommunity;
	private Label newRouterStatus;
	private Button newRouterSave;
	private Grid rgrid;
	private Window rwin;
	private Div newrwin;
	private Combobox carriertype;
	private Combobox netquerytype;
	private Textbox routerPassword;


	public final void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		rwin.setAttribute("rmvci", this);
	}

	public void onTimer$rtimer(Event e) {

		try {
			List<String[]> routerinfos = DataBase.getRoutersCached();
			Iterator<String[]> ri = routerinfos.iterator();
			while (ri.hasNext()) {
				String[] routerinfo = ri.next();
				String lid = "rr-" + routerinfo[DataBase.ROUTER_ID];
				Label sl = (Label) rwin.getFellowIfAny(lid);
				if (sl != null) {
					if (!sl.getValue().equals(routerinfo[DataBase.ROUTER_STATUSSTRING])) {
						sl.setValue(routerinfo[DataBase.ROUTER_STATUSSTRING]);
					}
				}
			}			
			
		} catch (Exception e1) {
			try {
				Messagebox.show(e1.getMessage(), null, Messagebox.OK,
						Messagebox.ERROR);
			} catch (InterruptedException e2) {
			}
		}
	}



	public void onClick$newRouterSave(Event e) {
		String srouterAddress = routerAddress.getValue();
		String srouterCommunity = routerCommunity.getValue();
		String srouterPass = routerPassword.getValue();

		int carry = 0;
		if (carriertype.getValue().equals("SNMP")) {
			carry = DataBase.ROUTER_TYPE_SNMP;
		} else if (carriertype.getValue().equals("Cisco CLI")) {
			carry = DataBase.ROUTER_TYPE_CISCOCLI;
		}

		int ipv4 = 0;
		int ipv6 = 0;
		if (netquerytype.getValue().equals("IPv4")) {
			ipv4 = 1;
		} else if (netquerytype.getValue().equals("IPv6")) {
			ipv6 = 1;
		} else if (netquerytype.getValue().equals("Both")) {
			ipv4 = ipv6 = 1;
		}
		
		try {
		DataBase.addRouter(srouterAddress, srouterCommunity, srouterPass, carry, ipv4, ipv6);
		} catch (Exception e1) {
			try {
				Messagebox.show(e1.getMessage(), null, Messagebox.OK,
						Messagebox.ERROR);
			} catch (InterruptedException e2) {
			}
		}
				

	}

	public void onRouterDel(Event e) {
		Button b = (Button) e.getTarget();
		int rrhashcode = Integer.parseInt(b.getId().replace("rrb-", ""));
		try {
			DataBase.delSwitch(rrhashcode);
		} catch (Exception e1) {
			try {
				Messagebox.show(e1.getMessage(), null, Messagebox.OK,
						Messagebox.ERROR);
			} catch (InterruptedException e2) {
			}
		}

		rgrid.setModel(rgrid.getModel());
	}

	public void onChange$carriertype(Event e) {
		if (carriertype.getValue().equals("SNMP")) {
			routerPassword.setDisabled(true);
		} else if (carriertype.getValue().equals("Cisco CLI")) {
			routerPassword.setDisabled(false);
		}
	}

}
