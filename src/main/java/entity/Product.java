package entity;

import java.time.LocalDate;

public class Product {
	private int       proid;
	private String    proname;
	private boolean   prostatus;
	private LocalDate promfg;
	private String    proimg;

	public Product() {
	}

	public Product(int proid, String proname, boolean prostatus,
		LocalDate promfg, String proimg) {
		this.proid     = proid;
		this.proname   = proname;
		this.prostatus = prostatus;
		this.promfg    = promfg;
		this.proimg    = proimg;
	}

	public int getProid() {
		return proid;
	}

	public void setProid(int proid) {
		this.proid = proid;
	}

	public String getProname() {
		return proname;
	}

	public void setProname(String proname) {
		this.proname = proname;
	}

	public boolean isProstatus() {
		return prostatus;
	}

	public void setProstatus(boolean prostatus) {
		this.prostatus = prostatus;
	}

	public LocalDate getPromfg() {
		return promfg;
	}

	public void setPromfg(LocalDate promfg) {
		this.promfg = promfg;
	}

	public String getProimg() {
		return proimg;
	}

	public void setProimg(String proimg) {
		this.proimg = proimg;
	}

	@Override
	public String toString() {
		return "Product [proid=" + proid
			+ ", proname=" + proname + ", prostatus="
			+ prostatus + ", promfg=" + promfg
			+ ", proimg=" + proimg + "]";
	}
}
