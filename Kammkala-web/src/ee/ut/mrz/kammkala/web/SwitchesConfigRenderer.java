package ee.ut.mrz.kammkala.web;


import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

public class SwitchesConfigRenderer implements RowRenderer {

	@Override
	public void render(Row row, Object data) throws Exception {
		
		String[] switchinfo = (String[]) data;
		String name = switchinfo[DataBase.SWITCH_NAME];
		String address = switchinfo[DataBase.SWITCH_IP];
		String community = switchinfo[DataBase.SWITCH_COMMUNITY];
		
		Label label1 = new Label(name);
		label1.setId("sname-" + switchinfo[DataBase.SWITCH_ID]);
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
		label4.setId("sr-" + switchinfo[DataBase.SWITCH_ID]);
		Cell cell4 = new Cell();
		label4.setParent(cell4);
		cell4.setParent(row);
		
		Label label5 = new Label("");
		label5.setId("sup-" + switchinfo[DataBase.SWITCH_ID]);
		Cell cell5 = new Cell();
		label5.setParent(cell5);
		cell5.setParent(row);
		
		Button b1 = new Button("Delete");
		b1.setId("srb-" + switchinfo[DataBase.SWITCH_ID]);
		
		b1.addEventListener(Events.ON_CLICK, new EventListener() {			
			@Override
			public void onEvent(Event event) throws Exception {				
				SwitchesMVC smvci = (SwitchesMVC) event.getTarget().getFellow("swin").getAttribute("smvci");
				smvci.onSwitchDel(event);				
			}
		});
		
		Cell cell6 = new Cell();
		b1.setParent(cell6);
		cell6.setParent(row);
			
	}

}
