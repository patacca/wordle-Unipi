package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.exceptions.AlreadyLoggedException;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.IOError;
import edu.riccardomori.wordle.client.backend.exceptions.InvalidUserException;
import edu.riccardomori.wordle.client.backend.exceptions.UnknownHostException;
import edu.riccardomori.wordle.client.frontend.GUI.ClientSession;
import edu.riccardomori.wordle.client.frontend.GUI.ViewManager;
import java.awt.Font;
import java.awt.GridBagConstraints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class LoginPanel extends JPanel {
    private JLabel errMsg;
    private JTextField usernameField;
    private JPasswordField passwordField;

    private ClientBackend backend;
    private ClientSession session;
    private ViewManager viewManager;

    public LoginPanel(ViewManager manager, ClientSession session, ClientBackend backend) {
        super();

        this.backend = backend;
        this.session = session;
        this.viewManager = manager;

        this.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // Add the title
        JLabel title = new JLabel("Wordle");
        title.setFont(new Font("NYTKarnakCondensed", Font.BOLD, 48));
        this.addComponent(title, gbc, 0, 0, 0, 0, GridBagConstraints.PAGE_START, 0, 0);

        // Add the subtitle
        JLabel textLabel = new JLabel("a game for smart people");
        textLabel.setFont(new Font("NYTKarnakCondensed", Font.BOLD, 22));
        this.addComponent(textLabel, gbc, 0, 1, 0, 1, GridBagConstraints.PAGE_START, 0, 0);

        // Add error message
        this.errMsg = new JLabel("Error");
        this.errMsg.setFont(new Font("Dialog", Font.PLAIN, 16));
        this.errMsg.setForeground(Color.RED);
        this.errMsg.setVisible(false);
        this.addComponent(this.errMsg, gbc, 0, 2, 0, 0, GridBagConstraints.CENTER, 0, 0);

        // Add the username label
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Ariel", Font.PLAIN, 18));
        this.addComponent(usernameLabel, gbc, 0, 3, 0, 0, GridBagConstraints.LINE_START, 0, 0,
                new Insets(30, 0, 0, 0));

        // Add the username field
        this.usernameField = new JTextField(25);
        this.addComponent(this.usernameField, gbc, 0, 4, 0, 1, GridBagConstraints.PAGE_START, 0, 10,
                new Insets(0, 0, 0, 0));

        // Add the password label
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Ariel", Font.PLAIN, 18));
        this.addComponent(passwordLabel, gbc, 0, 5, 0, 0, GridBagConstraints.LINE_START, 0, 0,
                new Insets(5, 0, 0, 0));

        // Add the password text input field
        this.passwordField = new JPasswordField(25);
        this.addComponent(this.passwordField, gbc, 0, 6, 0, 1, GridBagConstraints.PAGE_START, 0, 10,
                new Insets(0, 0, 0, 0));

        // Add the login button
        JButton loginButton = new JButton("Login");
        this.addComponent(loginButton, gbc, 0, 7, 0, 1, GridBagConstraints.PAGE_START, 0, 0,
                new Insets(30, 0, 0, 0));

        // Add the registration label
        JLabel registerLabel = new JLabel("Or click here to register a new user");
        registerLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
        this.addComponent(registerLabel, gbc, 0, 8, 0, 0, GridBagConstraints.PAGE_START, 0, 0,
                new Insets(40, 0, 0, 0));

        // Add the register button
        JButton registerButton = new JButton("Register");
        this.addComponent(registerButton, gbc, 0, 9, 0, 0, GridBagConstraints.CENTER, 0, 0,
                new Insets(0, 0, 0, 0));

        // Event listeners
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    LoginPanel.this.login();
                }
            }
        });
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginPanel.this.login();
            }
        });
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RegistrationPanel.popup(LoginPanel.this.backend);
            }
        });
    }

    private void addComponent(JComponent component, GridBagConstraints gbc, int gridx, int gridy,
            double weightx, double weighty, int anchor, int ipadx, int ipady) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;

        this.add(component, gbc);
    }

    private void addComponent(JComponent component, GridBagConstraints gbc, int gridx, int gridy,
            double weightx, double weighty, int anchor, int ipadx, int ipady, Insets insets) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;
        gbc.insets = insets;

        this.add(component, gbc);
    }

    /**
     * Perform the login operation
     */
    private void login() {
        String username = this.usernameField.getText().trim();
        String password = new String(this.passwordField.getPassword()).trim();
        if (username.length() > 255 || password.length() > 255) {
            this.error("Wrong username or password.");
            return;
        }

        // Call backend
        try {
            this.backend.login(username, password);

            // Update session
            this.session.login(username);

            // Change the view
            viewManager.switchTo(ViewManager.MAIN_VIEW);
        } catch (InvalidUserException e) {
            this.error("Wrong username or password.");
        } catch (AlreadyLoggedException e) {
            this.error(
                    "You are already logged in. Only one session per user is allowed at any time.");
        } catch (UnknownHostException e) {
            this.error("Hostname doesn't seem to exist.");
        } catch (IOError e) {
            this.error("I/O error during server communication.");
        } catch (GenericError e) {
            this.error("An error happened. Try again later.");
        }
    }

    private void error(String message) {
        this.usernameField.setBorder(BorderFactory.createLineBorder(Color.RED));
        this.passwordField.setBorder(BorderFactory.createLineBorder(Color.RED));

        this.errMsg.setText(message);
        this.errMsg.setVisible(true);

        this.viewManager.resync();
    }
}
