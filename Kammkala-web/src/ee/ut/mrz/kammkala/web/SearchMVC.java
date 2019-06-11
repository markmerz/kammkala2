package ee.ut.mrz.kammkala.web;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Row;

public class SearchMVC extends GenericForwardComposer {

	private Button searchButton;
	private Textbox timeTextbox;
	private Textbox macTextbox;
	private Textbox ipTextbox;
	private Textbox nameTextbox;
	private Label feedbackLabel;
	private Grid resultsGrid;

	public final void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
	}

	public void onClick$searchButton(Event e) throws SQLException, NamingException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		feedbackLabel.setStyle(null);
		feedbackLabel.setValue("");

		String timeText = timeTextbox.getValue().replace("'", "");
		String macText = macTextbox.getValue().replace("'", "");
		String ipText = ipTextbox.getValue().replace("'", "");
		String nameText = nameTextbox.getValue().replace("'", "");

		if (timeText != null && timeText.trim().equals("")) {
			timeText = null;
		}
		if (macText != null && macText.trim().equals("")) {
			macText = null;
		}
		if (ipText != null && ipText.trim().equals("")) {
			ipText = null;
		}
		if (nameText != null && nameText.trim().equals("")) {
			nameText = null;
		}

		java.sql.Timestamp timestamp = null;
		
		if (timeText != null) {
									
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd hh:mm");
			java.util.Date parsedDate = null;
			try {
				parsedDate = dateFormat.parse(timeText);
			} catch (ParseException e1) {
				feedbackLabel.setStyle("color:red");
				feedbackLabel.setValue(e1.getMessage());

				return;
			}
			timestamp = new java.sql.Timestamp(parsedDate.getTime());

			if (macText == null && ipText == null && nameText == null) {
				feedbackLabel.setStyle("color:red");
				feedbackLabel
						.setValue("Fill in at least one from MAC, IP or NAME");
				return;
			}

		}
		
		List<List<String>> searchResults = DataBase.getSearchResult(timestamp, macText, ipText, nameText);
		Iterator<List<String>> sri = searchResults.iterator();
		Rows rows = resultsGrid.getRows();
		rows.getChildren().clear();
		
		while (sri.hasNext()) {
			List<String> rrow = sri.next();
			
			Row row = new Row();
			row.setParent(rows);
			
			Cell c1 = new Cell();
			c1.setParent(row);
			Label l1 = new Label();
			l1.setParent(c1);
			l1.setValue(rrow.get(0));
			
			Cell c2 = new Cell();
			c2.setParent(row);
			Label l2 = new Label();
			l2.setParent(c2);
			l2.setValue(rrow.get(1));
			
			Cell c3 = new Cell();
			c3.setParent(row);
			Label l3 = new Label();
			l3.setParent(c3);
			l3.setValue(rrow.get(2));
			
			Cell c4 = new Cell();
			c4.setParent(row);
			Label l4 = new Label();
			l4.setParent(c4);
			l4.setValue(rrow.get(3));
			
			Cell c5 = new Cell();
			c5.setParent(row);
			Label l5 = new Label();
			l5.setParent(c5);
			l5.setValue(rrow.get(4));
			
		}
		
		resultsGrid.setVisible(true);
		
	}
}
