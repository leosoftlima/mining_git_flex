package br.ufpe.cin.GitMiner.io;

import au.com.bytecode.opencsv.CSVReader;
import br.ufpe.cin.GitMiner.Commits.BaseCommit;
import br.ufpe.cin.GitMiner.Commits.TaskCommit;
import br.ufpe.cin.GitMiner.ComputeChangeSet.ChangeSet;
import br.ufpe.cin.GitMiner.PotentialConflicts.GitMinerPotentialConflicts;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/*the input must contain the following informations:
 * the repository user's id
 * the repository name
 * baseCommit, key
 * baseName, key
 * an so on
 * 
 * */

/*The method readXMLFile was added to read the xml with the data to instantiate 
 * changesets with the base and task commit, download the commits from github and
 * unzip the commits, giving as a result a list of changesets
 * 
 * */

public class FileHandler {

    private static String getValue(String tag, Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodes.item(0);
        return node.getNodeValue();
    }

    public ChangeSet readXmlFile(String filePath) {
        ChangeSet result = new ChangeSet();
        File file = new File(filePath);
        DocumentBuilder dBuilder;

        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodes = doc.getElementsByTagName("commit");

            //changeset first commit
            Node baseNode = nodes.item(0);

            if (baseNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) baseNode;
                String nameBase = getValue("name", element);
                String urlBase = getValue("zipUrl", element);
                String shaKeyBase = getValue("SHAKey", element);
                BaseCommit base = new BaseCommit(nameBase, urlBase, shaKeyBase);
                result.setBaseCommit(base);
            }

            //changeset second commit
            Node taskNode = nodes.item(1);
            if (taskNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) taskNode;
                String nameCommit = getValue("name", element);
                String urlCommit = getValue("zipUrl", element);
                String shaKeyCommit = getValue("SHAKey", element);
                TaskCommit task = new TaskCommit(nameCommit, urlCommit, shaKeyCommit);
                result.setTaskCommit(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public GitMinerPotentialConflicts reader(String fileName) throws IOException {
        GitMinerPotentialConflicts result = new GitMinerPotentialConflicts(fileName);
        CSVReader reader = new CSVReader(new FileReader(fileName));
        List<String[]> entries = reader.readAll();

        //set GitMiner userId attribute
        String[] userLine = entries.get(0);
        String userId = userLine[0];
        result.setUserId(userId);

        //set GitMiner repo attribute
        String[] repoLine = entries.get(1);
        String repo = repoLine[0];
        result.setRepoName(repo);

        //set GitMiner base commit
        String[] baseLine = entries.get(2);
        String name = baseLine[0];
        String sha = baseLine[1];
        result.setBase(new BaseCommit(name, sha, sha));

        //set GitMiner tasks commits
        List<TaskCommit> tasks = new ArrayList<>();
        int tasksIndex = 3;
        while (tasksIndex < entries.size()) {
            String taskLine[] = entries.get(tasksIndex);
            name = taskLine[0];
            sha = taskLine[1];
            tasks.add(new TaskCommit(name, sha, sha));
            tasksIndex++;
        }
        result.setTasksCommits(tasks);

        return result;
    }

    public void downloader(GitMinerPotentialConflicts repo) throws IOException {
        String gitUrl = "https://github.com/" + repo.getUserId() + "/" +
                repo.getRepoName() + "/archive/";

        //download base commit
        String url = gitUrl + repo.getBase().getSHAKey() + ".zip";

        this.auxiliarDownload(url, repo.getBase().getName());
        System.out.println("end of " + repo.getBase().getName() + " download.");
        //download tasks commits

        for (TaskCommit f : repo.getTasksCommits()) {
            String fUrl = gitUrl + f.getSHAKey() + ".zip";
            this.auxiliarDownload(fUrl, f.getName());
            System.out.println("end of " + f.getName() + " download.");
        }

        System.out.println("done downloading");
    }

    public void auxiliarDownload(String url, String name) {
        BufferedInputStream in;
        try {
            in = new java.io.BufferedInputStream(new java.net.URL(url).openStream());
            FileOutputStream fos = new java.io.FileOutputStream(name + ".zip");
            BufferedOutputStream bout = new BufferedOutputStream(fos, 8192);
            byte[] data = new byte[8192];
            int x;
            while ((x = in.read(data, 0, 8192)) >= 0) {
                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unzipper(GitMinerPotentialConflicts repo) throws IOException {
        //unzip base
        String zipFile = repo.getBase().getName() + ".zip";
        String outputFolder = repo.getBase().getName();
        this.auxiliarUnzip(zipFile, outputFolder);
        System.out.println("done unzipping base commit.");

        //renames the root directory from rgms-shaKey to "soucecode"
        File oldName = new File(repo.getBase().getName() + File.separator + "rgms-" + repo.getBase().getSHAKey());
        File newName = new File(repo.getBase().getName() + File.separator + "sourcecode");
        oldName.renameTo(newName);

        //unzip other tasks
        for (TaskCommit f : repo.getTasksCommits()) {

            String fZipFile = f.getName() + ".zip";
            String fOutputFolder = f.getName();
            this.auxiliarUnzip(fZipFile, fOutputFolder);
            System.out.println("done unzipping task " + f.getName() + " commit.");

            //renames the root directory from rgms-shaKey to "soucecode"
            oldName = new File(f.getName() + File.separator + "rgms-" + f.getSHAKey());
            newName = new File(f.getName() + File.separator + "sourcecode");
            oldName.renameTo(newName);
        }
    }

    public void auxiliarUnzip(String zipFile, String outputFolder) {
        byte[] buffer = new byte[1024];
        File folder = new File(outputFolder); //create output directory is not exists

        if (!folder.exists()) {
            folder.mkdir();
            ZipInputStream zis; //get the zip file content

            try {
                zis = new ZipInputStream(new FileInputStream(zipFile));
                //get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();

                while (ze != null) {
                    String fileName = ze.getName();
                    File newFile = new File(outputFolder + File.separator + fileName);

                    // System.out.println("file unzip : "+ newFile.getAbsoluteFile());

                    //create all non exists folders
                    //else you will hit FileNotFoundException for compressed folder

                    if (ze.isDirectory()) {
                        new File(newFile.getParent()).mkdirs();
                    } else {
                        FileOutputStream fos;
                        new File(newFile.getParent()).mkdirs();
                        fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
                //System.out.println("Done unzipping");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writer(String fileName, String fileContent) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName + ".csv"));
            out.write(fileContent);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean compareTwoFiles(String baseFile, String taskFile) throws IOException {
        boolean result;
        String s1 = "";
        String s2 = "";
        String y, z;
        BufferedReader bfr = new BufferedReader(new FileReader(baseFile));
        BufferedReader bfr1 = new BufferedReader(new FileReader(taskFile));

        while ((z = bfr1.readLine()) != null)
            s2 += z;

        while ((y = bfr.readLine()) != null)
            s1 += y;

        result = s2.equals(s1);

        return result;
    }

}
