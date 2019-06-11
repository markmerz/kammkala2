package ee.ut.mrz.kammkala.web;


import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

public class RoutersConfigRenderer implements RowRenderer {

	@Override
	public void render(Row row, Object data) throws Exception {
		String[] routerinfo = (String[]) data;
		
		String name = routerinfo[DataBase.ROUTER_NAME];
		String address = routerinfo[DataBase.ROUTER_IP];
		String community = routerinfo[DataBase.ROUTER_COMMUNITY];
		String updated = routerinfo[DataBase.ROUTER_LASTUPDATE];
		
		Label label1 = new Label(name);
		Cell cell1 = new Cell();
		label1.setParent(cell1);
		cell1.setParent(row);
		
		Label label2 = new Label(address);
		Cell cell2 = new Cell();
		label2.setParent(cell2);
		cell2.setParent(row);
		
		Label label3 = new Label(community);
		Cell cell3 = new Cell();
		label3.setParent(cell3);
		cell3.setParent(row);
						
		Label label4 = new Label("");
		label4.setId("rr-" + routerinfo[DataBase.ROUTER_ID]);
		Cell cell4 = new Cell();
		label4.setParent(cell4);
		cell4.setParent(row);
		
		Label label6 = new Label(updated);
		Cell cell6 = new Cell();
		label6.setParent(cell6);
		cell6.setParent(row);
		
		Button b1 = new Button("Delete");
		b1.setId("rrb-" + routerinfo[DataBase.ROUTER_ID]);
		
		b1.addEventListener(Events.ON_CLICK, new EventListener() {			
			@Override
			public void onEvent(Event event) throws Exception {				
				RoutersMVC rmvci = (RoutersMVC) event.getTarget().getFellow("rwin").getAttribute("rmvci");
				rmvci.onRouterDel(event);				
			}
		});
		
		Cell cell5 = new Cell();
		b1.setParent(cell5);
		cell5.setParent(row);
			
	}

}
