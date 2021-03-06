package servlet;

import Database.FileDataStore;
import Database.CloudStore;
import Database.UserDataStore;
import Entity.Files;
import Entity.Users;
import Entity.permissionUpload;
//import mail.MailSender;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mail.MailSender;
import utils.EmptyFileGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Upload extends HttpServlet {
        FileDataStore fileManager = FileDataStore.getInstance();
        CloudStore cloud = new CloudStore();
        UserDataStore userStore = UserDataStore.getInstance();
        //////////////////////__JSON__///////////////////////////////
    /*
    {Action: “Upload”,
     Body: {
        userID: Bob@gmail.com
        filePath: “Bob/myVideos/”
        name: "video5.mp4"
        fileSize: 34.0
        type: video
        }
    }
    */
        /////////////////////////////////////////////////////////////
        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        }

        @Override
        public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            PrintWriter out = resp.getWriter();
            StringBuffer jb = new StringBuffer();
            EmptyFileGenerator gen = new EmptyFileGenerator();
            String line = null;
            try {
                BufferedReader reader = req.getReader();
                while ((line = reader.readLine()) != null) {
                    jb.append(line);
                }
            } catch (Exception e) { /*report an error*/ }
            try {
                JsonParser jparser = new JsonParser();
                JsonElement obj = jparser.parse(jb.toString());
                JsonObject jsontest = obj.getAsJsonObject();
                if (jsontest.get("Action").getAsString().equals("Upload")) {
                    JsonObject body = (JsonObject) jsontest.get("Body");
                    String name = body.get("name").getAsString();
                    String fileURL = body.get("filePath").getAsString();
                    String email = body.get("userID").getAsString();
                    double taille = body.get("fileSize").getAsDouble();
                    // byte[] file = java.nio.file.Files.readAllBytes(littefile);
                    byte[] file = gen.CreateLocalFile( body.get("fileSize").getAsInt());
                    String type = body.get("type").getAsString();
                    Users user = userStore.getUserbyMail(email);
                    String trueUrl = "";
                    if (user != null) {
                        String[] tab_req = user.getReq().split(",");
                        permissionUpload permission = new permissionUpload(user.getLevel());
                        if (permission.canSendRequest(tab_req)) {
                            tab_req = permission.tab_reqUpadted(tab_req);
                            switch (user.getLevel()) {
                                case "Noob":
                                    out.println("NOOB case");
                                    user.setReq(tab_req[0] + ",0,0,0");
                                    break;
                                case "Casual":
                                    out.println("Casual case");
                                    user.setReq(tab_req[0] + "," + tab_req[1] + ",0,0");
                                    break;
                                case "Leet":
                                    out.println("LEET case");
                                    user.setReq(tab_req[0] + "," + tab_req[1] + "," + tab_req[2] + "," + tab_req[3]);
                                    break;
                            }
                            trueUrl = "" + cloud.uploadFile(name, file);
                            fileManager.addFile(new Files(email, name, trueUrl, taille, type));
                            out.println("FILE POSTED ON :" + trueUrl);
                            user.setPoint(user.getPoint() + (long) taille);
                            userStore.updateUser(user);
                            out.println(" ----Upload is successful---- ");
                            MailSender.SendLinkTo(user.getEmail(), trueUrl);
                        } else {
                            out.println("lol non, vous devez attendre 1 min avant de lancer votre prochaine requete d'upload");
                            MailSender.SendLinkTo(user.getEmail(), "lol non, vous devez attendre 1 min avant de lancer votre prochaine requete d'upload");
                        }
                    }else{
                        out.println("User dont exist");
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
            out.println(e.toString());
        }
    }
}
