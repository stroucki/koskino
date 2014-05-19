/*
Copyright 2011-2013 Frederic Langlet
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package kanzi.test;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.util.QuadTreeGenerator;


/**
 *
 * @author fred
 */
public class TestQuadTreeGenerator
{

    public static void main(String[] args)
    {
        try
        {
            String fileName = (args.length > 0) ? args[0] : "c:\\temp\\lena.jpg";
            ImageIcon icon = new ImageIcon(fileName);
            Image image = icon.getImage();
            int w = image.getWidth(null) & -8;
            int h = image.getHeight(null) & -8;
            System.out.println(w+"x"+h);
            GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            img.getGraphics().drawImage(image, 0, 0, null);
            BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            int[] source = new int[w*h];
            int[] dest = new int[w*h];
            final List<QuadTreeGenerator.Node> nodes = new ArrayList<QuadTreeGenerator.Node>();

            // Do NOT use img.getRGB(): it is more than 10 times slower than
            // img.getRaster().getDataElements()
            img.getRaster().getDataElements(0, 0, w, h, source);
            System.arraycopy(source, 0, dest, 0, w*h);

            int nbNodes = 8;
            int minNodeDim = 8;
            nodes.clear();
            new QuadTreeGenerator(w, h, minNodeDim).decomposeNodes(nodes, source, 0, nbNodes);
            img2.getRaster().setDataElements(0, 0, w, h, dest);

            for (QuadTreeGenerator.Node node : nodes)
               img2.getGraphics().drawRect(node.x, node.y, node.w, node.h);

            //icon = new ImageIcon(img);
            final JFrame frame = new JFrame("Original");
            frame.setBounds(150, 100, w, h);
            frame.add(new JLabel(icon));
            frame.setVisible(true);
            final JFrame frame2 = new JFrame("Filter");
            frame2.setBounds(700, 150, w, h);
            ImageIcon newIcon = new ImageIcon(img2);
            frame2.add(new JLabel(newIcon));
            frame2.setVisible(true);
            frame2.addMouseMotionListener(new MouseAdapter()
            {
               @Override
               public void mouseMoved(MouseEvent e)
               {
                  QuadTreeGenerator.Node[] copy = nodes.toArray(new QuadTreeGenerator.Node[nodes.size()]);

                  for (QuadTreeGenerator.Node node : copy)
                  {
                     if (node == null)
                        continue;

                     if ((e.getX() >= node.x) && (e.getY() >= node.y)
                             && (e.getX() <= node.x+node.w)
                             && (e.getY() <= node.y+node.h))
                     {
                        frame2.setTitle("Filter - variance="+String.valueOf(node.variance) +
                               " - nodes="+String.valueOf(copy.length));
                     }
                  }
               }
            });

            try
            {
               while (nodes.size() < 1000)
               {
                  nodes.clear();
                  nbNodes++;
                  new QuadTreeGenerator(w, h, minNodeDim).decomposeNodes(nodes, source, 0, nbNodes);
                  img2.getRaster().setDataElements(0, 0, w, h, source);

                  for (QuadTreeGenerator.Node node : nodes)
                    img2.getGraphics().drawRect(node.x, node.y, node.w, node.h);

                  String title = frame2.getTitle();
                  int idx = title.lastIndexOf("- nodes=");

                  if (idx > 0)
                     title = title.substring(0, idx);

                  frame2.setTitle(title+"- nodes="+String.valueOf(nodes.size()));
                  frame2.invalidate();
                  frame2.repaint();
                  Thread.sleep(40);
               }

               Thread.sleep(40000);
            }
            catch (Exception e)
            {
              e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.exit(0);
    }
}

