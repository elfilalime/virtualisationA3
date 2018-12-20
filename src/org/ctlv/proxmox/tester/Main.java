package org.ctlv.proxmox.tester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.ctlv.proxmox.api.data.Node;
import org.json.JSONException;

import static java.lang.Thread.sleep;


public class Main {

	private static ProxmoxAPI api;

	public static void main(String[] args) throws LoginException, IOException, JSONException {

		api = new ProxmoxAPI();
		
		// Listes les CTs par serveur
		/*for (int i=5; i<=6; i++) {
			getServersInfo("srv-px"+i);
		}*/

		// demarer une CT
		//initStartCT("srv-px5","1301");

		//getCtInfo("srv-px5","1301");

		// generateur de ct avec contrainte
		CtGenerator();

		// gestinneur des ct selon contrainte
		ClusterManagement();

		//// Pour detruire nos CT

		ArrayList<LXC> cts5 = (ArrayList<LXC>) api.getCTs("srv-px5");
		ArrayList<LXC> cts6 = (ArrayList<LXC>) api.getCTs("srv-px6");

		for (int i = 0; i < cts5.size(); i++) {
			if(cts5.get(i).getVmid().matches("^13[0-9]{1,2}$")) {
				if(cts5.get(i).getStatus() == "running") {
					api.stopCT("srv-px5", cts5.get(i).getVmid());
					while (api.getCT("srv-px5", cts5.get(i).getVmid()).getStatus().equals("running")) ;
				}
				api.deleteCT("srv-px5", cts5.get(i).getVmid());
			}
		}

		for (int i = 0; i < cts6.size(); i++) {
			if(cts6.get(i).getVmid().matches("^13[0-9]{1,2}$")) {
				if(cts6.get(i).getStatus() == "running") {
					api.stopCT("srv-px6", cts6.get(i).getVmid());
					while (api.getCT("srv-px6", cts6.get(i).getVmid()).getStatus().equals("running")) ;
				}
				api.deleteCT("srv-px6", cts6.get(i).getVmid());
			}
		}

		//// Pour demarer nos CT
		/*ArrayList<LXC> cts5 = (ArrayList<LXC>) api.getCTs("srv-px5");
		ArrayList<LXC> cts6 = (ArrayList<LXC>) api.getCTs("srv-px6");

		for (int i = 0; i < cts5.size(); i++) {
			if(cts5.get(i).getVmid().matches("^13[0-9]{1,2}$")) {
				api.startCT("srv-px5", cts5.get(i).getVmid());
			}
		}

		for (int i = 0; i < cts6.size(); i++) {
			if(cts6.get(i).getVmid().matches("^13[0-9]{1,2}$")) {
				api.startCT("srv-px6", cts6.get(i).getVmid());
			}
		}*/
		
	}

	public static void ClusterManagement() throws LoginException, IOException, JSONException {
		Node px5 = api.getNode("srv-px5");
		Node px6 = api.getNode("srv-px6");

		ArrayList<LXC> cts5 = (ArrayList<LXC>) api.getCTs("srv-px5");
		ArrayList<LXC> cts6 = (ArrayList<LXC>) api.getCTs("srv-px6");

		int CountCts5 = cts5.size();

		while(!checkRunCtInServ(px5,cts5,12) && CountCts5 >=0 ){
			if(cts5.get(CountCts5-1).getVmid().matches("^13[0-9]{2}$")) {
				api.stopCT("srv-px5",cts5.get(CountCts5-1).getVmid());
			}
			CountCts5--;
		}

		int CountCts6 = cts6.size();

		while(!checkRunCtInServ(px6,cts6,12) && CountCts6 >=0 ){
			if(cts6.get(CountCts6-1).getVmid().matches("^13[0-9]{2}$")) {
				api.stopCT("srv-px6",cts6.get(CountCts6-1).getVmid());
			}
			CountCts6--;
		}

		CountCts5 = cts5.size();

		while(!checkRunCtInServ(px5,cts5,3) && CountCts5 >=0 ){
			if(cts5.get(CountCts5-1).getVmid().matches("^13[0-9]{2}$")) {
				api.stopCT("srv-px5",cts5.get(CountCts5-1).getVmid());
				while (cts5.get(CountCts5-1).getStatus().equals("running"));
				api.migrateCT("srv-px5",cts5.get(CountCts5-1).getVmid(),"srv-px6");
				while (cts6.get(CountCts5-1).getStatus().equals("running"));
			}
			CountCts5--;
		}

		CountCts6 = cts6.size();

		while(!checkRunCtInServ(px6,cts6,3) && CountCts6 >=0 ){
			if(cts6.get(CountCts6-1).getVmid().matches("^13[0-9]{2}$")) {
				api.migrateCT("srv-px6",cts6.get(CountCts6-1).getVmid(),"srv-px5");
			}
			CountCts6--;
		}
	}

	public static void CtGenerator() throws LoginException, IOException, JSONException {
		Node px5 = api.getNode("srv-px5");
		Node px6 = api.getNode("srv-px6");

		ArrayList<LXC> cts5 = (ArrayList<LXC>) api.getCTs("srv-px5");
		ArrayList<LXC> cts6 = (ArrayList<LXC>) api.getCTs("srv-px6");

		int idCt = 0;

		while (
				((checkCtInServ(px5,cts5,16) && getServCtsSize(cts5,cts6) <= 66)
						|| (checkCtInServ(px6,cts6,16) && getServCtsSize(cts6,cts5)<= 33))
						&& idCt <= 99
				){

			String servAleatoir = pileFace();

			String idFinale = String.valueOf(idCt);

			if(idFinale.length() == 1){
				idFinale = "0"+idFinale;
			}

			switch (servAleatoir){
				case "5":{
					if(checkCtInServ(px5,cts5,16) ){
						api.createCT("srv-px5","13"+idFinale,Constants.CT_BASE_NAME+idFinale,16);
					}else{
						api.createCT("srv-px6","13"+idFinale,Constants.CT_BASE_NAME+idFinale,16);
					}
					break;
				}
				case "6":{
					if(checkCtInServ(px6,cts6,16)){
						api.createCT("srv-px6","13"+idFinale,Constants.CT_BASE_NAME+idFinale,16);
					}else{
						api.createCT("srv-px5","13"+idFinale,Constants.CT_BASE_NAME+idFinale,16);
					}
					break;
				}
			}

			idCt++;

			cts5 = (ArrayList<LXC>) api.getCTs("srv-px5");
			cts6 = (ArrayList<LXC>) api.getCTs("srv-px6");

		}
	}

	public static void initStartCT(String serv,String ct) throws LoginException, IOException, JSONException {
		api.createCT(serv,ct,Constants.CT_BASE_NAME+"01",16);
		try {
			sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		api.startCT(serv,ct);
	}

	public static void getServersInfo(String srv) throws LoginException, IOException, JSONException {
		System.out.println("CTs sous "+srv);
		List<LXC> cts = api.getCTs(srv);

		System.out.println("CPU");
		System.out.println(api.getNode(srv).getCpu()*100+" %");
		System.out.println("RAM");
		System.out.println(Float.valueOf(api.getNode(srv).getMemory_used())/Float.valueOf(api.getNode(srv).getMemory_total())*100 + " %");
		System.out.println("Disque Dur");
		System.out.println(Float.valueOf(api.getNode(srv).getRootfs_used())/Float.valueOf(api.getNode(srv).getRootfs_total())*100 + " %");
	}

	public static void getCtInfo(String serv,String ct) throws LoginException, IOException, JSONException {
		System.out.println("Status");
		System.out.println(api.getCT(serv,ct).getStatus());

		System.out.println("CPU");
		System.out.println(api.getCT(serv,ct).getCpu()*100+" %");
		System.out.println("RAM");
		System.out.println(Float.valueOf(api.getCT(serv,ct).getMem())/Float.valueOf(api.getCT(serv,ct).getMaxmem())*100 + " %");
		System.out.println("Disque Dur");
		System.out.println(Float.valueOf(api.getCT(serv,ct).getDisk())/Float.valueOf(api.getCT(serv,ct).getMaxdisk())*100 + " %");

		System.out.println("Host Server");
		System.out.println(api.getCT(serv,ct).getName());
	}

	/**
	 * check que l'espace mémoire de tous nos conteneurs (trinôme A3) sur le serveur donné en paramètre
	 * est inférieur à la seuil de la capacité totale de ce dernier
	 * @param serv
	 * @param cts
	 * @param Seuil
	 * @return
	 */
	public static boolean checkCtInServ(Node serv,ArrayList<LXC> cts, int Seuil){
		float size = 0;

		for (int i = 0; i < cts.size(); i++) {
			if(cts.get(i).getVmid().matches("^13[0-9]{2}$")) {
				size = size + cts.get(i).getMaxmem();
			}
		}

		if( (Float.valueOf(size)/Float.valueOf(serv.getMemory_total()))*100 < Seuil){
			return true;
		}

		return false;
	}


	public static boolean checkRunCtInServ(Node serv,ArrayList<LXC> cts, int Seuil){
		float size = 0;

		for (int i = 0; i < cts.size(); i++) {
			if(cts.get(i).getVmid().matches("^13[0-9]{2}$")) {
				size = size + cts.get(i).getMem();
			}
		}

		if( (Float.valueOf(size)/Float.valueOf(serv.getMemory_used()))*100 < Seuil){
			return true;
		}

		return false;
	}
	/**
	 * calcule le % d'occupation des conteneurs du serveur cts1 par rapport à la somme de celle des CT sur les 2 serveurs
	 * @param cts1
	 * @param cts2
	 * @return
	 */
	public static float getServCtsSize(ArrayList<LXC> cts1,ArrayList<LXC> cts2){
		float size1 = 0;

		float size2 = 0;

		for (int i = 0; i < cts1.size(); i++) {
			if(cts1.get(i).getVmid().matches("^13[0-9]{2}$")) {
				size1 = size1 + cts1.get(i).getMaxmem();
			}
		}

		for (int i = 0; i < cts2.size(); i++) {
			if(cts2.get(i).getVmid().matches("^13[0-9]{2}$")) {
				size2 = size2 + cts2.get(i).getMaxmem();
			}
		}

		if(size1+size2 == 0){
			return 0;
		}
		return (Float.valueOf(size1)/Float.valueOf(size2+size1))*100;
	}


	public static String pileFace(){
		Random r = new Random();
		float Low =  0;
		float High = 1;
		float Result = Low + r.nextFloat()*(High-Low);

		if (Result <= 0.75){
			return "5";
		}else{
			return "6";
		}
	}
}
