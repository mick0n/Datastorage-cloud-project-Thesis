/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mnorrman.datastorageproject;

import com.mnorrman.datastorageproject.network.ConnectionContext;
import com.mnorrman.datastorageproject.network.MasterNode;
import com.mnorrman.datastorageproject.objects.ServerNode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.JPanel;

/**
 *
 * @author Mikael
 */
public class ServerMonitor extends javax.swing.JFrame implements Runnable{

    private MasterNode mn;
    private Painter painter;
    
    /**
     * Creates new form ServerMonitor
     */
    public ServerMonitor() {
        initComponents();
        setVisible(true);
    }
    
    public ServerMonitor(MasterNode mn) {
        this.mn = mn;
        initComponents();
        painter = new Painter();
        add(painter);
        pack();
        setVisible(true);
    }

    public void run() {
        while(true){
            if(mn != null){
                String[][] data;
                Collection<ServerNode> co = Main.slaveList.getAllData();
                data = new String[co.size()][3];
                Iterator<ServerNode> it = co.iterator();
                int index = 0;
                while(it.hasNext()){
                    ServerNode sn = it.next();
                    data[index][0] = sn.getId();
                    if(sn.getState() != null)
                        data[index][1] = sn.getState().toString();
                    else
                        data[index][1] = "invalid";
                    data[index][2] = sn.getDataSize() + " bytes";
                    index++;
                }
                painter.update(data);
            }
            repaint();
            try{
                Thread.sleep(1000);
            }catch(InterruptedException e){
                LogTool.log(e, LogTool.CRITICAL);
            }
        }
    }
 
    private class Painter extends JPanel{
        
        private String[][] nodes;
        
        public Painter(){
            super();
            setPreferredSize(new Dimension(400, 500));
            setVisible(true);
        }

        public void update(String[][] newNodes){
            this.nodes = newNodes;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 500);
            g.setColor(Color.BLACK);
            g.drawString(0x000000 + " - " + Main.state, 5, 20);
            if(nodes != null)
                for(int a = 0; a < nodes.length; a++){
                    g.drawString("0x" + nodes[a][0] + " - " + nodes[a][1] + ", " + nodes[a][2], 5, 40+(20*a));
                }
        }
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Server monitor");
        getContentPane().setLayout(new java.awt.FlowLayout());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServerMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ServerMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ServerMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ServerMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new ServerMonitor().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
