/**
 * Created by paulo on 07/05/2017.
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.*;

/**
 * @se http://stackoverflow.com/a/20603012/230513
 * @see http://stackoverflow.com/a/17763395/230513
 */
public class McScratchLauncher {

    private final JLabel statusLabel = new JLabel("Status: ", JLabel.CENTER);
    private final JTextArea mcpiServerOutputTextArea = new JTextArea(20, 60);
    private final JTextArea mcServerOutputTextArea = new JTextArea(20, 60);
    private JButton startButton = new JButton("Start");
    private JButton stopButton = new JButton("Stop");
    private JProgressBar bar = new JProgressBar();
    private BackgroundTask mcServerRunner;
    private BackgroundTask mcpiServerRunner;
    private JTextField cmdinput = new JTextField();
    private ArrayList<String> cmdhistory = new ArrayList<>();
    private int cmdhistoryIdx;

    public McScratchLauncher(){
        start();
    }

    private final ActionListener buttonActions = event -> {
        JButton source = (JButton) event.getSource();
        if (source == startButton) {
            start();
        } else if (source == stopButton) {
            stop();
        }
    };

    private void stop() {
        mcServerRunner.cancel(true);
        mcServerRunner.done();
        mcpiServerRunner.cancel(true);
        mcpiServerRunner.done();
    }

    private void start()  {

        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();

        mcServerOutputTextArea.setText(null);
        mcpiServerOutputTextArea.setText(null);

        Font consolasFont = new Font("Consolas", Font.PLAIN, 12);
        mcServerOutputTextArea.setFont( consolasFont );
        mcpiServerOutputTextArea.setFont( consolasFont );
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        String serverCwd = cwd + "/server";

        String javaw = System.getProperty("java.home") + "/bin/javaw";
        ProcessBuilder mcpiServerProcessBuilder = new ProcessBuilder(javaw, "-jar", serverCwd + "\\jython\\jython.jar", serverCwd + "\\mcpi-scratch.py");
        mcpiServerRunner = new BackgroundTask( mcpiServerProcessBuilder, serverCwd , mcpiServerOutputTextArea);
        mcpiServerRunner.execute();

        ProcessBuilder mcServerProcessBuilder = new ProcessBuilder(javaw, "-Xmx1024M","-Xms1024M","-jar", getFirstJarFile(serverCwd), "nogui" , "--jline","false");
        mcServerRunner = new BackgroundTask( mcServerProcessBuilder, serverCwd , mcServerOutputTextArea);
        mcServerRunner.execute();

        bar.setIndeterminate(true);
    }

    private String getFirstJarFile(String mcServerCwd) {
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream( Paths.get(mcServerCwd), "*.jar");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.iterator().next().toAbsolutePath().toString();
    }

    private void cmdhistoryWalkBack(){
        cmdhistoryWalk( 1 );
    }

    private void cmdhistoryWalkForward(){
        cmdhistoryWalk( -1 );
    }

    private void cmdhistoryWalk( int step ){
        cmdhistoryIdx += step;
        if(cmdhistoryIdx >= cmdhistory.size() - 1 ){
            cmdhistoryIdx = 0;
        }
        if(cmdhistoryIdx < 0 ) {
            cmdhistoryIdx = cmdhistory.size() - 1 ;
        }

        cmdinput.setText( cmdhistory.get( cmdhistory.size() - 1 - cmdhistoryIdx ) );
    }

    private void setCmdInputKey( String key, AbstractAction action){
        KeyStroke keyStroke = KeyStroke.getKeyStroke(key);
        cmdinput.getInputMap().put(keyStroke, key);
        cmdinput.getActionMap().put(key, action);
    }

    private void displayGUI() {
        JFrame frame = new JFrame("Swing Worker Example");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setLayout(new BorderLayout(5, 5));



        JPanel mcpiServerPanel = new JPanel();
        mcpiServerPanel .setBorder(BorderFactory.createTitledBorder("MCPI-Scratch Server Output: "));
        JScrollPane mcpiServerOutputPanel = new JScrollPane();
        mcpiServerOutputPanel.setViewportView(mcpiServerOutputTextArea);
        mcpiServerPanel.setLayout(new BorderLayout(5, 5));
        mcpiServerPanel.add( mcpiServerOutputPanel, BorderLayout.CENTER );


        JPanel mcServerPanel = new JPanel();
        mcServerPanel.setBorder(BorderFactory.createTitledBorder("Minecraft Server Output: "));
        JScrollPane mcServerOutputPanel = new JScrollPane();
        mcServerOutputPanel.setViewportView(mcServerOutputTextArea);
        mcServerPanel.setLayout(new BorderLayout(5, 5));
        mcServerPanel.add( mcServerOutputPanel, BorderLayout.CENTER );


        cmdinput.addActionListener( ev -> {
            String cmd = ev.getActionCommand();
            try {
                BufferedWriter stdin = mcServerRunner.getStdin();
                stdin.write( cmd + '\n');
                stdin.flush();
            } catch (IOException e) {
                System.err.println("Error sending command to Minecraft server console.");
                e.printStackTrace();
            }
            cmdhistory.add(cmd);
            cmdhistoryIdx = 0;
            cmdinput.setText("");
        });

        setCmdInputKey( "UP", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmdhistoryWalkBack();
            }
        });

        setCmdInputKey( "DOWN", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmdhistoryWalkForward();
            }
        });

        mcServerPanel.add(cmdinput,BorderLayout.SOUTH);


        JPanel serverConsolesPanel = new JPanel();
        serverConsolesPanel.setLayout( new GridLayout( 0, 2 ) );
        serverConsolesPanel.add( mcServerPanel );
        serverConsolesPanel.add( mcpiServerPanel );


        startButton.addActionListener(buttonActions);
        stopButton.addActionListener(buttonActions);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(bar);


        panel.add(statusLabel, BorderLayout.PAGE_START);
        panel.add(serverConsolesPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.PAGE_END);


        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                mcServerRunner.done();
                mcpiServerRunner.done();
            }
        });


    }

    private class BackgroundTask extends SwingWorker<Integer, String> {

        private int status;
        private Process proc;
        private ProcessBuilder processBuilder = null;
        private String cwd = null;
        private JTextArea outputTextArea = null;

        public BufferedWriter getStdin() {
            return stdin;
        }

        private BufferedWriter stdin;

        public BackgroundTask( ProcessBuilder processBuilder, String cwd, JTextArea outputTextArea ) {
            this.processBuilder = processBuilder;
            this.cwd = cwd;
            this.outputTextArea = outputTextArea;
            statusLabel.setText((this.getState()).toString());
        }

        @Override
        protected Integer doInBackground() {
            try {
                processBuilder.directory( new File(cwd) );
                processBuilder.redirectErrorStream(true);
                proc = processBuilder.start();
                stdin = new BufferedWriter( new OutputStreamWriter( proc.getOutputStream() ));
                BufferedReader stdout = new BufferedReader( new InputStreamReader(proc.getInputStream()));
                BufferedReader stderr = new BufferedReader( new InputStreamReader(proc.getErrorStream()));

                String e = null;
                String s = null;
                while ( ((e = stderr.readLine()) != null || (s = stdout.readLine()) != null) && !isCancelled()) {

                    if( e != null ){
                        publish(e);
                    }
                    if( s != null ) {
                        publish(s);
                    }
                }

                if (!isCancelled()) {
                    status = proc.waitFor();
                }

                killProc();
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
            return status;
        }

        private void killProc() {
            try {
                proc.getOutputStream().close();
                proc.getErrorStream().close();
                proc.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void process(java.util.List<String> messages) {
            statusLabel.setText((this.getState()).toString());
            for (String message : messages) {
                outputTextArea.append(message + "\n");
            }
        }

        @Override
        protected void done() {
            killProc();
            statusLabel.setText((this.getState()).toString() + " " + status);
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            bar.setIndeterminate(false);
        }

    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new McScratchLauncher().displayGUI());
    }
}