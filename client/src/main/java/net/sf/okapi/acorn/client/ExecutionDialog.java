package net.sf.okapi.acorn.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class ExecutionDialog extends JDialog implements ActionListener, PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private final JEditorPane edNote;
	private final JButton btStart;
	private final JProgressBar pbProgress;
	private final JTextField edText;

	private XLIFFDocumentTask task;
	private boolean done = false;

	public ExecutionDialog (JFrame owner,
		boolean modal)
	{
		super(owner, modal);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		
		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		pbProgress = new JProgressBar(0, 100);
		pbProgress.setStringPainted(true);
		c.gridx = 0; c.gridy = 0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(pbProgress, c);

		c = new GridBagConstraints();
		edText = new JTextField();
		edText.setEditable(false);
		c.gridx = 0; c.gridy = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(edText, c);

		c = new GridBagConstraints();
		btStart = new JButton("Start");
		btStart.setActionCommand("start");
		btStart.addActionListener(this);
		c.gridx = 0; c.gridy = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(btStart, c);

		c = new GridBagConstraints();
		edNote = new JEditorPane();
		edNote.setContentType("text/html");
		c.anchor = GridBagConstraints.PAGE_END;
		c.gridx = 0; c.gridy = 3;
		c.weightx = 1.0; c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = GridBagConstraints.REMAINDER;
		panel.add(edNote, c);

		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		add(panel, BorderLayout.CENTER);

		resetUI();

		pack();
		setMinimumSize(getPreferredSize());
		setPreferredSize(new Dimension(700, 500));
		pack();
		
		setLocationRelativeTo(owner);
	}

	private void resetUI () {
		done = false;
		btStart.setText("Start");
		pbProgress.setValue(0);
		edText.setText("Click Start to start the process");
	}

	public void setTask (XLIFFDocumentTask task,
		String title)
	{
		setTitle(title);
		edNote.setText(task.getInfo());
		this.task = task;
		resetUI();
	}
	
	@Override
	public void propertyChange (PropertyChangeEvent event) {
		switch ( event.getPropertyName() ) {
		case "progress":
			int progress = (Integer)event.getNewValue();
			pbProgress.setValue(progress);
			edText.setText(String.format("Completed %d%% of task...", task.getProgress()));
			break;
		case "state":
			if ( "DONE".equals(event.getNewValue().toString()) ) {
				done();
			}
			break;
		}
	}

	@Override
	public void actionPerformed (ActionEvent event) {
		if ( done ) { // Close
			setVisible(false);
		}
		else { // Start
			btStart.setEnabled(false);
			//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			task.addPropertyChangeListener(this);
			task.execute();
		}
	}

	private void done () {
		done = true;
		pbProgress.setValue(pbProgress.getMaximum());
		btStart.setText("Close");
		btStart.setEnabled(true);
		if ( task.getError() != null ) edText.setText("Error(s) occurred: See the log.");
		else edText.setText("Done");
	}

}
