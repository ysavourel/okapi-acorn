package net.sf.okapi.acorn.client;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.okapi.acorn.xom.Factory;
import net.sf.okapi.acorn.xom.json.JSONWriter;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.oasisopen.xliff.om.v1.ISegment;
import org.oasisopen.xliff.om.v1.IXLIFFFactory;

public class APITestPanel extends JPanel {

	private final static String BASEURL = "http://localhost:8080/taustapi/v2"; 
	private final static IXLIFFFactory xf = Factory.XOM;
	private final static JSONWriter jw = new JSONWriter();
	
	private JList lbMethods;
	private JTextField edId;
	private JTextField edCallbackUrl;
	private JTextField edSrcLang;
	private JTextField edTrgLang;
	private JTextField edSource;
	private JTextField edTarget;
	private JTextArea edResult;
	
	public APITestPanel () {
		GridBagLayout layout =  new GridBagLayout();
		setLayout(layout);
		
		String[] methods = {
			/* 0 */ "GET translation - get a list of all translation requests",
			/* 1 */ "POST translation - create a new translation request",
			/* 2 */ "GET translation/{id} - get an existing translation request",
			/* 3 */ "PUT translation/{id} - modify an existing translation request",
			/* 4 */ "DELETE translation/{id} - delete an existing translation request",
			/* 5 */ "GET status/{id} - get the status of a translation request",
			/* 6 */ "PUT accept/{id}",
			/* 7 */ "PUT reject/{id}",
			/* 8 */ "PUT confirm/{id}",
			/* 9 */ "PUT cancel/{id}"};
		lbMethods = new JList(methods);
		lbMethods.setSelectedIndex(0);
		lbMethods.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged (ListSelectionEvent event) {
				updateCommand();
			}
		});
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 0.10;
		add(new JLabel("Command:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.90;
		add(lbMethods, c);
		JButton btExecute = new JButton("Execute");
		btExecute.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed (ActionEvent event) {
				execute();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 3; c.gridy = 0;
		//c.weightx = 0.90;
		add(btExecute, c);
		
		c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 0.10;
		add(new JLabel("Request ID:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 1; c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.90; c.gridwidth = GridBagConstraints.REMAINDER;
		add((edId = new JTextField()), c);
		
		c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 2; c.anchor = GridBagConstraints.LINE_START;
		add(new JLabel("Callback URL:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 2; c.fill = GridBagConstraints.HORIZONTAL;
		 c.gridwidth = GridBagConstraints.REMAINDER;
		add((edCallbackUrl = new JTextField()), c);
		
		c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 3; c.anchor = GridBagConstraints.LINE_START;
		add(new JLabel("Source language:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 3; c.fill = GridBagConstraints.HORIZONTAL;
		 c.gridwidth = GridBagConstraints.REMAINDER;
		add((edSrcLang = new JTextField()), c);
		edSrcLang.setText("en");
		
		c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 4; c.anchor = GridBagConstraints.LINE_START;
		add(new JLabel("Target language:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 4; c.fill = GridBagConstraints.HORIZONTAL;
		 c.gridwidth = GridBagConstraints.REMAINDER;
		add((edTrgLang = new JTextField()), c);
		edTrgLang.setText("iu");

		c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 5; c.anchor = GridBagConstraints.LINE_START;
		add(new JLabel("Source text:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 5; c.fill = GridBagConstraints.HORIZONTAL;
		 c.gridwidth = GridBagConstraints.REMAINDER;
		add((edSource = new JTextField()), c);

		c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 6; c.anchor = GridBagConstraints.LINE_START;
		add(new JLabel("Target text:"), c);
		c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 6; c.fill = GridBagConstraints.HORIZONTAL;
		 c.gridwidth = GridBagConstraints.REMAINDER;
		add((edTarget = new JTextField()), c);
		
		edResult = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(edResult);
		edResult.setLineWrap(true);
		edResult.setWrapStyleWord(true);
		Font font = new Font("Gadugi", 0, 20); // Gadugi, Euphemia //new Font("Courier New", 0, 20);
		edResult.setFont(font);
		c = new GridBagConstraints(); c.anchor = GridBagConstraints.PAGE_END;
		c.gridx = 0; c.gridy = 7; c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		 c.gridwidth = GridBagConstraints.REMAINDER;
		add(scrollPane, c);
		
		updateCommand();
	}

	private void updateCommand () {
		switch ( lbMethods.getSelectedIndex() ) {
		case 0: // GET translation
			enableFields(false, false, false);
			break;
		case 1: // POST translation
			enableFields(true, false, true);
			break;
		case 3: // PUT translation/{id}
			enableFields(true, true, true);
			break;
		default:
			enableFields(true, false, false);
		}
	}
	
	private void enableFields (boolean id,
		boolean target,
		boolean others)
	{
		edId.setEnabled(id);
		edCallbackUrl.setEnabled(others);
		edSrcLang.setEnabled(others);
		edTrgLang.setEnabled(others);
		edSource.setEnabled(others);
		edTarget.setEnabled(target);
	}

	private void execute () {
		edResult.setText("Executing the command...");
		Response resp = null;
		StringBuilder jtmp;
		ISegment seg;
		Client cli;
		MultiPart multiPartEntity;
		
		try {
			Client client = ClientBuilder.newClient();
			WebTarget target;
			switch ( lbMethods.getSelectedIndex() ) {
			case 0: // GET translation
				target = client.target(BASEURL).path("translation");
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
				break;
			case 1: // POST translation
				jtmp = new StringBuilder("{\"translationRequest\"{");
				jtmp.append("\"id\":\""+edId.getText()+"\",");
				jtmp.append("\"sourceLanguage\":\""+edSrcLang.getText()+"\",");
				jtmp.append("\"targetLanguage\":\""+edTrgLang.getText()+"\",");
				jtmp.append("\"source\":\""+edSource.getText()+"\",");
				// XLIFF content
				seg = xf.createLoneSegment();
				seg.setSource(edSource.getText());
				jtmp.append("\"xlfSource\":"+jw.fromContent(seg.getSource()).toJSONString());
				// End of payload
				jtmp.append("}}");
				cli = ClientBuilder.newBuilder()
			    	.register(MultiPartFeature.class)
			    	.build();
				target = cli.target(BASEURL).path("translation");
				multiPartEntity = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE).bodyPart(
					new BodyPart(jtmp.toString(), MediaType.APPLICATION_JSON_TYPE));
				resp = target.request().post(Entity.entity(multiPartEntity, multiPartEntity.getMediaType())); 
				break;
			case 2: // GET translation/{id}
				target = client.target(BASEURL).path("translation/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
				break;
			case 3: // PUT translation/{id}
				jtmp = new StringBuilder("{\"translationRequest\"{");
				jtmp.append("\"id\":\""+edId.getText()+"\",");
				jtmp.append("\"sourceLanguage\":\""+edSrcLang.getText()+"\",");
				jtmp.append("\"targetLanguage\":\""+edTrgLang.getText()+"\",");
				jtmp.append("\"source\":\""+edSource.getText()+"\",");
				jtmp.append("\"target\":\""+edTarget.getText()+"\",");
				// XLIFF content
				seg = xf.createLoneSegment();
				seg.setSource(edSource.getText());
				jtmp.append("\"xlfSource\":"+jw.fromContent(seg.getSource()).toJSONString());
				// End of payload
				jtmp.append("}}");
				cli = ClientBuilder.newBuilder()
			    	.register(MultiPartFeature.class)
			    	.build();
				target = cli.target(BASEURL).path("translation/"+edId.getText());
				multiPartEntity = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE).bodyPart(
					new BodyPart(jtmp.toString(), MediaType.APPLICATION_JSON_TYPE));
				resp = target.request().put(Entity.entity(multiPartEntity, multiPartEntity.getMediaType())); 
				break;
			case 4: // DELETE translation/{id}
				target = client.target(BASEURL).path("translation/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).delete();
				break;
			case 5: // GET status/{id}
				target = client.target(BASEURL).path("status/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
				break;
			case 6: // PUT accept/{id}
				target = client.target(BASEURL).path("accept/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).put(Entity.text(""));
				break;
			case 7: // PUT reject/{id}
				target = client.target(BASEURL).path("reject/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).put(Entity.text(""));
				break;
			case 8: // PUT confirm/{id}
				target = client.target(BASEURL).path("confirm/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).put(Entity.text(""));
				break;
			case 9: // PUT cancel/{id}
				target = client.target(BASEURL).path("cancel/"+edId.getText());
				resp = target.request(MediaType.APPLICATION_JSON_TYPE).put(Entity.text(""));
				break;
			}

			StringBuilder tmp = new StringBuilder();
			tmp.append("Result: "+resp.getStatus()+"\n");
			tmp.append(resp.readEntity(String.class));
			edResult.setText(tmp.toString());
		}
		catch ( Throwable e ) {
			edResult.setText("Exception:\n"+e.getMessage());
		}
		
	}
	
}
