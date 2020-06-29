//FTP, est un protocole de communication destiné au partage de fichiers sur un réseau TCP/IP

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class client_ftp {

   private String ipAdress, dIpAdress;
   private int port, dPort;
   private String user;
   
   private Socket socket, dsocket;
   
   private BufferedInputStream BufferedInputStream, d_BufferedInputStream;
   private BufferedWriter BufferedWriter, d_BufferedWriter;
   
   //constructeur
   
   public client_ftp(String ipAddress, int Port, String User){
	  ipAdress = ipAddress;
	  port = Port;
	  user = User;
   }
   
   
   // Méthode de connexion au FTP
   
   public void connect() throws IOException{
	   
      //Si la socket est déjà initialisée
      if(socket != null)
           throw new IOException("La connexion est déjà activée");
      
      //On se connecte
      socket = new Socket(ipAdress, port);
      
      //On crée nos objets pour pouvoir communiquer (lire et ecrire)
      BufferedInputStream = new BufferedInputStream(socket.getInputStream());   
      BufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));   
      
      String reponse = read();      

      if(!reponse.startsWith("220")) 
         throw new IOException("Erreur de connexion au FTP : \n" + reponse);
  
      send("USER " + user);
      reponse = read();
      if(!reponse.startsWith("331"))  
         throw new IOException("Erreur de connexion avec le compte utilisateur : \n" + reponse);
      
      String passwd = "ninanour1998";
      send("PASS " + passwd);
      reponse = read();
      if(!reponse.startsWith("230"))  
         throw new IOException("Erreur de connexion avec le compte utilisateur : \n" + reponse);

      //Nous sommes maintenant connectés
   }
   
 //Méthode permettant de lire les réponses du serveur FTP
   /**
    * 
    * @return
    * @throws IOException
    */
   private String read() throws IOException{      
      String reponse = "";
      int stream;
      byte[] b = new byte[4096];
      stream = BufferedInputStream.read(b);
      reponse = new String(b, 0, stream);
      System.out.println("> " + reponse);
      return reponse;
   }
   
  //Méthode permettant d'envoyer les commandes au serveur FTP
   /**
    * 
    * @param command
    * @throws IOException
    */
   private void send(String command) throws IOException{
      command += "\r\n";
      System.out.println("> " + command);
      BufferedWriter.write(command);
      BufferedWriter.flush();
   }
   
   //la partie des commandes 
   
   //Methode qui permet de deconnecter de la session en cours
   private void QUIT() {
	 try {
	   send("QUIT");
	   } catch (IOException e) {
	     e.printStackTrace();
	     } finally{
	       if(socket != null){
	         try {
	             socket.close();
	             } catch (IOException e) {
	               e.printStackTrace();
	             }
	             }
	      }
   }
   
   //Methode pour afficher le chemin courante sur le serveur FTP
   private String PWD() throws IOException{
	      send("PWD");
	      return read();
   }
   
   //Methode pour aller a un repertoire choisi
   private String CWD(String repertoire) throws IOException{
	      send("CWD " + repertoire);
	      return read();
   }
   
   //Methode pour entrez en mode passive
   //En utilisant le mode passif, c'est le serveur FTP qui va déterminer le port qui sera 
   //utilisé pour le transfert des données
   private void PASV() throws IOException {
	   send("PASV");
	   String reponse = read();
	   String passive_ipadress = null;
	   int passive_port = -1;
	     
	   //On prend la réponse retournée par le serveur pour récupérer l'adresse 
	   //IP et le port à utiliser pour le canal data
	   int d = reponse.indexOf('(');
	   int f = reponse.indexOf(')', d + 1);
	   if(d > 0){
	   String dataLink = reponse.substring(d + 1, f);
	   StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
	   try {
	     //L'adresse IP est séparée par des virgules on les remplace donc par des points
	     passive_ipadress = tokenizer.nextToken() + "." + tokenizer.nextToken() + "."
	                + tokenizer.nextToken() + "." + tokenizer.nextToken();
	     
	     //Le port est un entier de type int mais cet entier est découpé en deux
	     //la première partie correspond aux 4 premiers bits de l'octet la deuxième au 
	     //4 derniers
	     passive_port = Integer.parseInt(tokenizer.nextToken()) * 256
	          + Integer.parseInt(tokenizer.nextToken());
	     dIpAdress = passive_ipadress;
	     dPort = passive_port; 
	          
	     System.out.println("> " + reponse);
	     } catch (Exception e) {
	       throw new IOException("could not generate a  data canal: " + reponse);
	       }        
	     }
   }
   
   //on recupere l'adresse Ip et le port envoyer par le server pour creer une 
   //nouvelle socket
   private void dataSocket() throws UnknownHostException, IOException{
	      dsocket = new Socket(dIpAdress, dPort);
	      d_BufferedInputStream = new BufferedInputStream(dsocket.getInputStream());
	      d_BufferedWriter = new BufferedWriter(new OutputStreamWriter(dsocket.getOutputStream()));
	   }
   
   //Methode pour entrez en mode ASCII pour que la commende LIST fonctionne
   private String ASCII() throws IOException {
     send("TYPE ASCII");      
     return read();
   }
  
   //Methode pour retournez le contenu du repertoire courant
   private String list() throws IOException{
      ASCII();
	  PASV();
	  dataSocket();
	  send("LIST");
	  return dataread();
   }
   
   
   /**
    * @return
    * @throws IOException
    */
   private String dataread() throws IOException{
	      String reponse = "";
	      byte[] b = new byte[4096];
	      int stream;
	      stream = d_BufferedInputStream.read(b);
	      reponse = new String(b, 0, stream);
	      System.out.println("> " + reponse);
          return reponse;
   }
	   
    //La partie des requetes
   //Requete pour l'obtention d'un fichier    
   private void retreiveFile(String filename) throws SocketException, IOException
   {
	   PASV();
       dataSocket();
       InputStream i = dsocket.getInputStream();

       send("RETR " +filename);
       read();
       
       // Get the size of the file
       File f = new File(filename);
       FileOutputStream fout = new FileOutputStream(f);
       byte[] b = new byte[4096];
       int byteread;
       while ((byteread = i.read(b)) > 0)
       {
    	fout.write(b,0,byteread);
       }
       fout.close();
	   i.close();
       dsocket.close();
       socket.close();
    }
   
    //Requete pour l'envoi d'un fichier
    private void storeFile(String filenaame) throws SocketException, IOException
    {
	  PASV();
      dataSocket();
      OutputStream o = dsocket.getOutputStream();
      
      send("STOR "+filenaame);
      read();
      
      // Get the size of the file
      File f = new File(filenaame);
      FileInputStream fin = new FileInputStream(f);
      byte[] b = new byte[4096];
      int byteread;
      while ((byteread = fin.read(b)) > 0)
      {
   	   o.write(b,0,byteread);
      }
      fin.close();
      o.close();
      dsocket.close();
      socket.close();
    }
  
    //Requete pour la suppression d'un fichier sur le serveur
    private void deleteFile(String file) throws SocketException, IOException
    {
       send("DELE "+file);
       read();
       socket.close();
    }
   
    private void afficheContenu(String rep) throws SocketException, IOException
    {
	  CWD(rep);
      list();
      socket.close();
    }
   
  
   public static void main(String[] args) {

	      try {
	         Scanner sc = new Scanner(System.in);
	         client_ftp c = new client_ftp("127.0.0.1", 21, "nourhouda");
	         System.out.println("Connexion au serveur FTP Filezilla");
             c.connect();
	         System.out.println("Vous êtes maintenant connecté au serveur FTP Filezilla");
	         System.out.println("Maintenant vous avez le droit aux commande et aux requetes \n "
	         		+ "just tapez le nom de commande ou requete que vous voulez : \n"
	         		+ " les commande PWD, CWD, PASV, LIST, QUIT \n "
	         		+ "les requetes retreiveFile, storeFile, deleteFile, LS -LA");
	         String reponse = "";
	         boolean command = true;
	         while (command) {
		         reponse = sc.nextLine().toString();

               switch (reponse) {
	              case "PWD":
	          	    c.PWD();
	                break;
	              case "CWD":
	          	    System.out.println("> Saisir le nom du répertoire que vous voulez aller : ");
	                String repertoire = sc.nextLine();
	                c.CWD(repertoire);
	                break;
	              case "PASV":
	              	c.PASV();
	              	break;
	              case "LIST":
	          	    c.list();
	                break;
	              case "QUIT":
	          	    c.QUIT();
	          	    command = false;
	                break;
	              case "retreiveFile":
	                System.out.println("> Saisir le nom du fichier que vous voulez obtenir : ");
		            String filename = sc.nextLine();
		            c.retreiveFile(filename);
		            break;
	              case "storeFile":
	            	System.out.println("> Saisir le nom du fichier que vous voulez envoyer : ");
			        String filenaame = sc.nextLine();
			        c.storeFile(filenaame);
			        break;
	              case "deleteFile":
	            	System.out.println("> Saisir le nom du fichier que vous voulez supprimer : ");
				    String file = sc.nextLine();
				    c.deleteFile(file);
				    break;  
	              case "LS -LA":
	            	System.out.println("> Saisir le nom du repertoire que vous voulez : ");
					String rep = sc.nextLine();
				    c.afficheContenu(rep);
					break; 
	              default : 
	                System.err.println("Commande inconnue !");
	               
	                   break;
	               }

	            }
	        
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(0);

	      }
	      System.out.println("fin de connection.");
	   }
            
  }






