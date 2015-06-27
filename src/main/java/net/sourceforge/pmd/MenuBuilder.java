/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import bluej.extensions.BClass;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

public class MenuBuilder extends MenuGenerator {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private Frame frame;
    private String javaFileName;
    private Preferences preferences;

    public MenuBuilder(Frame frame, Preferences preferences) {
        this.frame = frame;
        this.preferences = preferences;
    }

    public JMenuItem getClassMenuItem(BClass aClass) {
    try {
          javaFileName = aClass.getJavaFile().getPath(); 
           } catch (ProjectNotOpenException pnoe) {
           } catch (PackageNotFoundException pnfe) {
        }
        JMenu jm = new JMenu("PMD");
        jm.add(new JMenuItem(new SimpleAction("Check code")));
        return jm;
    }

    // The nested class that instantiates the different (simple) menus.
    class SimpleAction extends AbstractAction {

        public SimpleAction(String menuName) {
            putValue(AbstractAction.NAME, menuName);
        }

        public void actionPerformed(ActionEvent anEvent) {
            String pmdPath = preferences.getPMDPath();
            if (pmdPath == null || pmdPath.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "The path to PMD Installation is not configured. "
                        + "Please select the path under \"Tools / Preferences / Extensions / PMD\".",
                        "No Path to PMD Installation", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                JOptionPane.showMessageDialog(frame, "Running PMD on selected Class (Click OK)");
                String mycommand = preferences.getPMDPath() + "/bin/run.sh pmd " + preferences.getPMDOptions() + " -d " + javaFileName;

                if (SystemUtils.isWindows()) {
                    JOptionPane.showMessageDialog(frame, "Any errors will be displayed in command window with line numbers, press key to exit"); 
                    mycommand = "cmd /c start " + preferences.getPMDPath() + "\\bin\\pmd.bat " + preferences.getPMDOptions() + " -d " + javaFileName;
                }

                String output = runPMD(mycommand);

                JOptionPane.showMessageDialog(frame, "Class Checked");

                if (!SystemUtils.isWindows()) {
                    StringBuilder msg = new StringBuilder("Any problems found are displayed below:");
                    msg.append(LINE_SEPARATOR);
                    msg.append(output);
                    JOptionPane.showMessageDialog(frame, msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Couldn't run PMD: " + e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Couldn't run PMD: " + e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            }
            JOptionPane.showMessageDialog(frame, "PMD run completed");
        }

        private String runPMD(String mycommand) throws IOException, InterruptedException {
            ProcessBuilder pb = new ProcessBuilder(mycommand.split(" +"));
            pb.redirectErrorStream(true);
            final Process p = pb.start();

            final StringBuilder output = new StringBuilder();
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String s;
                    try {
                        while ((s = stdInput.readLine()) != null ){ 
                            output.append(s);
                            output.append(LINE_SEPARATOR);
                        }
                    } catch (IOException e) {
                        output.append(e.toString());
                        e.printStackTrace();
                    } finally {
                        try { stdInput.close(); } catch (IOException e) { /* quiet */ }
                    }
                }
            });
            reader.setDaemon(true);
            reader.start();
            p.waitFor();
            return output.toString();
        }
    }
}
