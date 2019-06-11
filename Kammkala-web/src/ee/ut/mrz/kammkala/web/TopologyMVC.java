package ee.ut.mrz.kammkala.web;



import java.sql.SQLException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

public class TopologyMVC extends GenericForwardComposer {
	private Grid tgrid;
	
	
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		@SuppressWarnings("rawtypes")
		Iterator fi = tgrid.getFellows().iterator();
		while (fi.hasNext()) {
			Object f = fi.next();
			if (f instanceof Button) {
				Button b = (Button) f;
				b.addEventListener("onClick", this);
			}
		}				
	}
	
	public void onClick(Event event) {
		Component c = event.getTarget();
		if (c instanceof Button) {
			Button b = (Button) c;			
			String time = b.getId().replaceAll("button_", "");
			String filename = "topology" + time.replace(" ", "_") + ".dot";
			try {
				String topology = DataBase.getTopology(time);
				Filedownload.save(topology, "application/x-graphviz", filename);
			} catch (Exception e) {
				try {
					Messagebox.show(e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR, null);
				} catch (InterruptedException e1) {
					
				}
			}
		}
	}
	
}
