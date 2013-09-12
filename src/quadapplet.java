import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import javax.swing.Timer;

import org.mbed.RPC.*;

public class quadapplet extends Applet implements mbedRPC, ActionListener {

    HTTPRPC mbed;
    boolean threadSuspended;
    Timer refresh_timer;
    private static final long serialVersionUID = 1L;

    // setup local and rpc variables
    RPCVariable<Integer> CtrlAction;
    RPCVariable<Character> LEDStatus0;
    RPCVariable<Character> LEDStatus1;
    RPCVariable<Character> LEDStatus2;

    // screen position copordinates for drawing LEDs, Btns and text
    int LED1_x = 20;
    int LED2_x = 80;
    int LED3_x = 140;
    int LED4_x = 180;
    int LED5_x = 200;
    int LED1_y = 21; // L/R
    int LED2_y = 91; // LNB status
    int LED3_y = 135; // LNB A/M
    int LED4_y = 172; // LNB Recal
    int LED5_y = 231; // UHF status
    int LED6_y = 275; // UHF A/M
    int LED7_y = 312; // UHF Recal
    int LED8_y = 373; // PSU status
    int LED_dx = 40;
    int LED2_dx = 60;
    int LED_dy = 28;
    int LED2_dy = 22;
    int LED_r = 6;

    int LEDStatus0_i = 0;
    int LEDStatus1_i = 0;
    int LEDStatus2_i = 0;
    int LocalActiveLED_i = 0;
    int LNBPosition_i = 0;
    int UHFPosition_i = 0;
    int LNBAutoManual_i = 0;
    int UHFAutoManual_i = 0;

    int LNBRecal_i = 0;
    int UHFRecal_i = 0;
    int LNBRecalCounter = 0;
    int UHFRecalCounter = 0;

    int PSU1Alarm_i = 0;
    int PSU2Alarm_i = 0;
    int LNB1Alarm_i = 0;
    int LNB2Alarm_i = 0;
    int UHF1Alarm_i = 0;
    int UHF2Alarm_i = 0;

    int LNBSwitchError_i = 0;
    int UHFSwitchError_i = 0;

    int CtrlStatusData = 0;
    int CommsOpenFlag = 0;
    int comms_active = 0;
    int connection_ctr = 0;
    int update_ctr = 0;

    int rate = 1500;

    Button LocalActive_ALBtn;
    Button Refresh_ALBtn;
    Button LNBSelect_ALBtn;
    Button LNBAutoManual_ALBtn;
    Button LNBRecal_ALBtn;
    Button UHFSelect_ALBtn;
    Button UHFAutoManual_ALBtn;
    Button UHFRecal_ALBtn;

    // **************************************************************************
    // * function to initialise
    // *
    public void init() {

        setLayout(null);

        mbed = new HTTPRPC(this);

        LEDStatus1 = new RPCVariable<Character>(mbed, "RemoteLEDStatus1"); // won't work with bool
        LEDStatus0 = new RPCVariable<Character>(mbed, "RemoteLEDStatus0");
        LEDStatus2 = new RPCVariable<Character>(mbed, "RemoteLEDStatus2");
        CtrlAction = new RPCVariable<Integer>(mbed, "RemoteCtrlAction");

        LEDStatus0_i = LEDStatus0.read_char();
        CommsOpenFlag = ((LEDStatus1_i >> 1) & 0x00000001);

        if (CommsOpenFlag == 0) {

            comms_active = 1;

            LocalActive_ALBtn = new Button("Local / Remote");
            Refresh_ALBtn = new Button("Update Connection Data");
            LNBSelect_ALBtn = new Button("LNB");
            LNBAutoManual_ALBtn = new Button("LNB Auto/Manual");
            LNBRecal_ALBtn = new Button("LNB Recalibrate");
            UHFSelect_ALBtn = new Button("UHF");
            UHFAutoManual_ALBtn = new Button("UHF Auto/Manual");
            UHFRecal_ALBtn = new Button("UHF Recalibrate");

            LocalActive_ALBtn.setBounds(80, 20, 160, 30);
            Refresh_ALBtn.setBounds(20, 420, 220, 30);
            LNBSelect_ALBtn.setBounds(97, 86, 66, 40);
            LNBAutoManual_ALBtn.setBounds(80, 131, 160, 30);
            LNBRecal_ALBtn.setBounds(80, 168, 160, 30);
            UHFSelect_ALBtn.setBounds(97, 226, 66, 40);
            UHFAutoManual_ALBtn.setBounds(80, 271, 160, 30);
            UHFRecal_ALBtn.setBounds(80, 308, 160, 30);

            add(LocalActive_ALBtn);
            add(Refresh_ALBtn);
            add(LNBSelect_ALBtn);
            add(LNBAutoManual_ALBtn);
            add(LNBRecal_ALBtn);
            add(UHFSelect_ALBtn);
            add(UHFAutoManual_ALBtn);
            add(UHFRecal_ALBtn);

            LocalActive_ALBtn.addActionListener(this);
            Refresh_ALBtn.addActionListener(this);
            LNBSelect_ALBtn.addActionListener(this);
            LNBAutoManual_ALBtn.addActionListener(this);
            LNBRecal_ALBtn.addActionListener(this);
            UHFSelect_ALBtn.addActionListener(this);
            UHFAutoManual_ALBtn.addActionListener(this);
            UHFRecal_ALBtn.addActionListener(this);

            refresh_timer = new Timer(rate, timerListener);
            refresh_timer.start();

            get_data();

            CtrlAction.write(0x01); // 01=Set Remote Comms Open/Active

        } else {
            comms_active = 0;
        }

    }

    // **************************************************************************
    // * functions for timer and memory control
    // *
    public void stop() {

        CtrlAction.write(0x02); // 01=Set Remote Comms off
        refresh_timer.stop();

        mbed.delete();
        super.destroy();
    }

    public void destroy() {

        CtrlAction.write(0x02); // 01=Set Remote Comms off

        super.destroy();
        mbed.delete();
    }

    // **************************************************************************
    // * function to get data from mbed RPC variable
    // *
    public void get_data() {

        LEDStatus0_i = LEDStatus0.read_char();
        LEDStatus1_i = LEDStatus1.read_char();
        LEDStatus2_i = LEDStatus2.read_char();

        LocalActiveLED_i = ((LEDStatus0_i >> 2) & 0x000000001);
        LNBPosition_i = ((LEDStatus0_i >> 3) & 0x000000001);
        UHFPosition_i = ((LEDStatus0_i >> 4) & 0x000000001);
        LNBAutoManual_i = ((LEDStatus0_i >> 5) & 0x000000001);
        UHFAutoManual_i = ((LEDStatus0_i >> 6) & 0x000000001);

        PSU1Alarm_i = ((LEDStatus1_i >> 1) & 0x000000001);
        PSU2Alarm_i = ((LEDStatus1_i >> 2) & 0x000000001);
        LNB1Alarm_i = ((LEDStatus1_i >> 3) & 0x000000001);
        LNB2Alarm_i = ((LEDStatus1_i >> 4) & 0x000000001);
        UHF1Alarm_i = ((LEDStatus1_i >> 5) & 0x000000001);
        UHF2Alarm_i = ((LEDStatus1_i >> 6) & 0x000000001);

        LNBSwitchError_i = ((LEDStatus2_i >> 1) & 0x000000001);
        UHFSwitchError_i = ((LEDStatus2_i >> 2) & 0x000000001);

    }

    // **************************************************************************
    // * function to be called for each refresh iteration
    // *
    ActionListener timerListener = new ActionListener() {
        public void actionPerformed(ActionEvent ev) {

            connection_ctr = connection_ctr + 1;
            if (connection_ctr >= 999) {
                connection_ctr = 0;
            }

            if (LNBRecalCounter > 0) {
                LNBRecalCounter = LNBRecalCounter - 1;
            }
            if (UHFRecalCounter > 0) {
                UHFRecalCounter = UHFRecalCounter - 1;
            }

            repaint();
        }
    };

    // **************************************************************************
    // * function to setup graphics and paint (empty)
    // *
    public void paint(Graphics g) {

        g.setColor(Color.blue);
        g.drawRoundRect(1, 1, 260, 460, 20, 20);

        Font smallFont = new Font("Arial", Font.PLAIN, 13);

        if (comms_active == 1) {

            g.setFont(smallFont);
            g.setColor(Color.black);

            g.drawString("PSU1", 74, 393);
            g.drawString("PSU2", 160, 393);

            // Draw Local/Remote LED and fill if active
            if (LocalActiveLED_i == 0) {
                g.setColor(Color.orange);
            } else {
                g.setColor(Color.white);
            }
            g.fillRoundRect(LED1_x, LED1_y, LED_dx, LED_dy, LED_r, LED_r);
            g.setColor(Color.orange);
            g.drawRoundRect(LED1_x, LED1_y, LED_dx, LED_dy, LED_r, LED_r);

            // Draw LNB LEDs and fill if active
            if (LNBPosition_i == 0) {
                g.setColor(Color.green);
            } else {
                g.setColor(Color.white);
            }
            if (LNB1Alarm_i >= 1) {
                g.setColor(Color.red);
            }
            if (LNBSwitchError_i >= 1) {
                g.setColor(Color.white);
            }
            g.fillRoundRect(LED1_x, LED2_y, LED2_dx, LED_dy, LED_r, LED_r);
            if (LNBSwitchError_i >= 1) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.green);
            }
            g.drawRoundRect(LED1_x, LED2_y, LED2_dx, LED_dy, LED_r, LED_r);
            g.setColor(Color.black);
            g.drawString("MAIN", LED1_x + 12, LED2_y + 20);
            if (LNBPosition_i >= 1) {
                g.setColor(Color.green);
            } else {
                g.setColor(Color.white);
            }
            if (LNB2Alarm_i >= 1) {
                g.setColor(Color.red);
            }
            if (LNBSwitchError_i >= 1) {
                g.setColor(Color.white);
            }
            g.fillRoundRect(LED4_x, LED2_y, LED2_dx, LED_dy, LED_r, LED_r);
            if (LNBSwitchError_i >= 1) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.green);
            }
            g.drawRoundRect(LED4_x, LED2_y, LED2_dx, LED_dy, LED_r, LED_r);
            g.setColor(Color.black);
            g.drawString("BACK", LED4_x + 12, LED2_y + 20);
            // Draw LNB Auto Manual
            if (LNBAutoManual_i >= 1) {
                g.setColor(Color.green);
                g.fillRoundRect(LED1_x, LED3_y, LED_dx, LED2_dy, LED_r, LED_r);
                g.setColor(Color.black);
                g.drawString("A", LED1_x + 14, LED3_y + 17);
            } else {
                g.setColor(Color.orange);
                g.fillRoundRect(LED1_x, LED3_y, LED_dx, LED2_dy, LED_r, LED_r);
                g.setColor(Color.black);
                g.drawString("M", LED1_x + 14, LED3_y + 17);
            }

            // Draw LNB Recal and fill if alive
            g.setColor(Color.white);
            if (LocalActiveLED_i == 0) {
                if (LNBRecalCounter >= 1) {
                    g.setColor(Color.orange);
                }
            }
            g.fillRoundRect(LED1_x, LED4_y, LED_dx, LED2_dy, LED_r, LED_r);
            g.setColor(Color.orange);
            g.drawRoundRect(LED1_x, LED4_y, LED_dx, LED2_dy, LED_r, LED_r);

            // Draw UHF LEDs and fill if active
            // UHF Main
            if (UHFPosition_i == 0) {
                g.setColor(Color.green);
            } else {
                g.setColor(Color.white);
            }
            if (UHF1Alarm_i >= 1) {
                g.setColor(Color.red);
            }
            if (UHFSwitchError_i >= 1) {
                g.setColor(Color.white);
            }
            g.fillRoundRect(LED1_x, LED5_y, LED2_dx, LED_dy, LED_r, LED_r);
            if (UHFSwitchError_i >= 1) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.green);
            }
            g.drawRoundRect(LED1_x, LED5_y, LED2_dx, LED_dy, LED_r, LED_r);
            g.setColor(Color.black);
            g.drawString("MAIN", LED1_x + 12, LED5_y + 20);
            // UHF Standby
            if (UHFPosition_i >= 1) {
                g.setColor(Color.green);
            } else {
                g.setColor(Color.white);
            }
            if (UHF2Alarm_i >= 1) {
                g.setColor(Color.red);
            }
            if (UHFSwitchError_i >= 1) {
                g.setColor(Color.white);
            }
            g.fillRoundRect(LED4_x, LED5_y, LED2_dx, LED_dy, LED_r, LED_r);
            if (UHFSwitchError_i >= 1) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.green);
            }
            g.drawRoundRect(LED4_x, LED5_y, LED2_dx, LED_dy, LED_r, LED_r);
            g.setColor(Color.black);
            g.drawString("BACK", LED4_x + 12, LED5_y + 20);
            // Draw LNB Auto Manual
            if (UHFAutoManual_i >= 1) {
                g.setColor(Color.green);
                g.fillRoundRect(LED1_x, LED6_y, LED_dx, LED2_dy, LED_r, LED_r);
                g.setColor(Color.black);
                g.drawString("A", LED1_x + 14, LED6_y + 17);
            } else {
                g.setColor(Color.orange);
                g.fillRoundRect(LED1_x, LED6_y, LED_dx, LED2_dy, LED_r, LED_r);
                g.setColor(Color.black);
                g.drawString("M", LED1_x + 14, LED6_y + 17);
            }
            // Draw UHF Recal and fill if alive
            g.setColor(Color.white);
            if (LocalActiveLED_i == 0) {
                if (UHFRecalCounter >= 1) {
                    g.setColor(Color.orange);
                }
            }
            g.fillRoundRect(LED1_x, LED7_y, LED_dx, LED2_dy, LED_r, LED_r);
            g.setColor(Color.orange);
            g.drawRoundRect(LED1_x, LED7_y, LED_dx, LED2_dy, LED_r, LED_r);

            // Draw PSU1 Alarm LED and fill red if error
            if (PSU1Alarm_i > 0) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.green);
            }
            g.fillRoundRect(LED1_x, LED8_y, LED_dx, LED_dy, LED_r, LED_r);

            // Draw PSU2 Alarm LED and fill red if error
            if (PSU2Alarm_i > 0) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.green);
            }
            g.fillRoundRect(LED5_x, LED8_y, LED_dx, LED_dy, LED_r, LED_r);

            g.setColor(Color.gray);
            g.setFont(smallFont);
            g.drawString(String.valueOf(connection_ctr), 270, 460);

        } else {

            g.setFont(smallFont);
            g.setColor(Color.black);
            g.drawString("Connection Error:", 50, 80);
            g.drawString("Comms Already In Use", 50, 100);

        }

    }

    // Here we ask which component called this method

    public void actionPerformed(ActionEvent evt) {

        if (evt.getSource() == LocalActive_ALBtn) {
            CtrlAction.write(0x03); // LR on
            CtrlAction.write(0x04); // LR off
            get_data();
            repaint();
            get_data();
            repaint();
        }

        if (evt.getSource() == LNBSelect_ALBtn) {
            CtrlAction.write(0x05); // LNB on
            get_data();
            repaint();
            get_data();
            repaint();
        }
        if (evt.getSource() == LNBAutoManual_ALBtn) {
            CtrlAction.write(0x07); // LNB Auto on
            get_data();
            repaint();
            get_data();
            repaint();
        }
        if (evt.getSource() == LNBRecal_ALBtn) {
            // LNBRecal_i=1;
            LNBRecalCounter = 3;
            // get_data();
            repaint();
            CtrlAction.write(0x09); // LNB Recal on
            get_data();
            repaint();
        }

        if (evt.getSource() == UHFSelect_ALBtn) {
            CtrlAction.write(0x0B); // UHF on
            get_data();
            repaint();
            get_data();
            repaint();
        }
        if (evt.getSource() == UHFAutoManual_ALBtn) {
            CtrlAction.write(0x0D); // UHF Auto on
            get_data();
            repaint();
        }
        if (evt.getSource() == UHFRecal_ALBtn) {
            // UHFRecal_i=1;
            UHFRecalCounter = 3;
            repaint();
            CtrlAction.write(0x0F); // UHF Recal on
            get_data();
            repaint();
        }

        if (evt.getSource() == Refresh_ALBtn) {
            get_data();
            repaint();
            get_data();
            repaint();
        }
    }
}