package ee.ut.mrz.kammkala.web;

import java.sql.SQLException;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;

public class DatabaseSettingsMVC extends GenericForwardComposer {
	
	private static final long serialVersionUID = 3697512811871319083L;
	
	private Label jndiStatus;
	private Label dbSchema;
	private Button schemaButton;
	
		
	public final void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
//		try {
			DataBaseConnection.getInstance();
			jndiStatus.setValue("Lookup succeeded");
			
			if (DataBaseConnection.getInstance().checkSchema()) {
				dbSchema.setValue("Schema check succeeded");
				dbSchema.setStyle(null);
			} else {
				schemaButton.setDisabled(false);
				dbSchema.setValue("No schema");
				dbSchema.setStyle("color:red");
			}
			
//		} catch (NamingException e) {			
//			jndiStatus.setValue("Lookup failed");
//			jndiStatus.setStyle("color:red");
//		}
		
		
	}
				
	public void onClick$schemaButton(Event e) throws InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
		try {
			DataBaseConnection.getInstance().generateSchema();
			if (DataBaseConnection.getInstance().checkSchema()) {
				dbSchema.setValue("Schema check succeeded");
				dbSchema.setStyle(null);
			} else {
				schemaButton.setDisabled(false);
				dbSchema.setValue("No schema");
				dbSchema.setStyle("color:red");
			}
		} catch (SQLException e1) {
			dbSchema.setStyle("color:red");
			dbSchema.setValue(e1.getMessage());
		}
//		catch (NamingException e1) {
//			dbSchema.setStyle("color:red");
//			dbSchema.setValue(e1.getMessage());
//		}
	}
		
	public static boolean isDbFailed() {		
		try {
			DataBaseConnection.getInstance();			
			
			if (DataBaseConnection.getInstance().checkSchema()) {
				return false;
			} else {
				return true;
			}			
		} catch (Exception e) {
			return true;
		} 
		
	}

}
