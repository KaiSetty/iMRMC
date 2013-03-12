package mrmc.core;

/*statistical tests. 
 * Hillis tests and Z test are implemented
 * Hillis tests require non-central F distribution. The formula is from
 *http://www.mathworks.com/help/toolbox/stats/brn2ivz-110.html 
 * */
import umontreal.iro.lecuyer.probdist.FisherFDist;
import umontreal.iro.lecuyer.probdist.BetaDist;
import umontreal.iro.lecuyer.probdist.NormalDist;
//import umontreal.iro.lecuyer.probdist.StudentDist;
import java.lang.Math;


public class statTest {
	int INFINITY = 500;
	int PRECISION = 6;
	double ZERO = 1E-300;
	double powerF;
	double powerF2011;
	double powerZ;
	double DOF = 0;
	double DOF2011 = 0;
	double tStat = 0;
	double effSize;
	double obsDiff;
	double sigLevel;
	double pValZ;
	double pValF;
	double ciBot, ciTop;
	double totalVar;
	double CVF;
	double df2;
	double Delta;

	public double getciBot() {
		return ciBot;
	}

	public double getciTop() {
		return ciTop;
	}

	public double getHillisPower() {
		return powerF;
	}

	public double getHillis2011() {
		return powerF2011;
	};

	public double getZPower() {
		return powerZ;
	}

	public double getDOF() {
		return DOF;
	}

	public double gettStat() {
		return tStat;
	}

	public double getpValZ() {
		return pValZ;
	}

	public double getpValF() {
		return pValF;
	}

	public double getCVF() {
		return CVF;
	}

	public double getDelta() {
		return Delta;
	}

	public double getDDF() {
		return df2;
	}

	public statTest(double[] var, double[] OR, int r, int c, double sig,
			double eff, double obsDif, double totalVar0) {
		sigLevel = sig;
		effSize = eff;
		obsDiff = obsDif;
		totalVar = totalVar0;
		powerF = FTest(var, r, c, totalVar);
		powerZ = ZTest(var, r, c, totalVar);
	}

	public statTest(dbRecord curRecord, int selectedMod, int useBiasM, int N2,
			int N0, int N1, double sig, double eff) {
		double mst = 0, denom = 0, statT = 0, ddf = 0;
		double[] aucs = { 0, 0 };
		matrix mx = new matrix();
		double[][] MS = curRecord.getMS(useBiasM);
		double[][] coeff = curRecord.genBDGCoeff(N2, N0, N1);
		double[][] BDG = curRecord.getBDG(useBiasM);

		System.out.println("***********");
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 8; j++) {
				System.out.println(coeff[i][j] + "    ");
			}
			System.out.println("\n");
		}
		// double
		// dn0=(coeff[selectedMod][2]+coeff[selectedMod][1])/coeff[selectedMod][1];
		// double
		// dn1=(coeff[selectedMod][3]+coeff[selectedMod][1])/coeff[selectedMod][1];
		// double
		// dnr=(coeff[selectedMod][5]+coeff[selectedMod][1])/coeff[selectedMod][1];
		// double dnc=dn0+dn1;

		double dn0 = curRecord.getNormal() * 1.0;
		double dn1 = curRecord.getDisease() * 1.0;
		double dnr = curRecord.getReader() * 1.0;
		double dnc = dn0 + dn1;

		double[] var = mx.dotProduct(BDG[selectedMod], coeff[selectedMod]);
		double var0 = 0;
		for (int j = 0; j < 8; j++)
			var0 = var0 + var[j];

		aucs[0] = curRecord.getAUCinNumber(0);
		aucs[1] = curRecord.getAUCinNumber(1);
		double meanCI = 0;
		if (selectedMod == 0 || selectedMod == 1) {
			meanCI = aucs[selectedMod];
			// mst=dnr*dnc*(aucs[selectedMod]-eff)*(aucs[selectedMod]-eff);
			mst = dnr * dnc * eff * eff;
			denom = MS[selectedMod][0]
					+ max(0.0, (MS[selectedMod][1] - MS[selectedMod][4]));
			statT = Math.sqrt(mst / denom);
			ddf = denom * denom
					/ (MS[selectedMod][0] * MS[selectedMod][0] / (dnr - 1));
		} else {
			meanCI = aucs[0] - aucs[1];
			mst = dnr * dnc * doVar(aucs[0], aucs[1]);
			denom = MS[selectedMod][2]
					+ max(0.0, (MS[selectedMod][3] - MS[selectedMod][5]));
			statT = Math.sqrt(mst / denom);
			ddf = denom * denom
					/ (MS[selectedMod][2] * MS[selectedMod][2] / (dnr - 1));
			System.out.println("R=" + dnr + "N0=" + dn0 + "N1=" + dn1 + "eff="
					+ eff + "sig" + sig + "auc0=" + aucs[0] + "auc1=" + aucs[1]
					+ "df=" + ddf);
			System.out.println("denom=" + denom + "mst=" + mst);
		}
		tStat = statT;
		DOF = ddf;
		FisherFDist fdist = new FisherFDist(1, (int) ddf);
		pValF = fdist.cdf(1, (int) ddf, 5, statT * statT);
		pValF = 1 - pValF;
		double Fval = fdist.inverseF(1 - sig);

		ciBot = meanCI - Math.sqrt(var0) * Math.sqrt(Fval); // bdg
		ciTop = meanCI + Math.sqrt(var0) * Math.sqrt(Fval); // bdg
		// ***
		// StudentDist tdist = StudentDist((int)ddf);
		// double pValT=tdist.inverse((int)ddf, 1-sig/2); //
		// ciBot=meanCI-Math.sqrt(var[selectedMod])*pValT; //bdg
		// ciTop=meanCI+Math.sqrt(var[selectedMod])*pValT; //bdg
		// ***

		System.out.println("auc(0)=" + meanCI + "Fval=" + Math.sqrt(Fval)
				+ "std" + Math.sqrt(var[selectedMod]));
		// System.out.println("selectedMod="+selectedMod+" mst="+mst+" denom="+denom+" tstat="+statT+" df="+ddf+"Fval="+Fval);

	}

	public double doVar(double a, double b) {
		double mean = (a + b) / 2.0;
		double var = (a - mean) * (a - mean) + (b - mean) * (b - mean);
		return var;
	}

	public double max(double a, double b) {
		if (a > b)
			return a;
		else
			return b;
	}

	public double FTest(double[] var, int r, int c, double totalVar) {
		double SigTR = var[0];
		double SigTC = var[1];
		double SigTRC = var[2];

		// print, SigTRC, SigTR, SigTC
		/*
		 * SigTR=SigTR; SigTC=SigTC*c; SigTRC=SigTRC; SigTRC=SigTRC*c;
		 */

		SigTR = SigTR;
		SigTC = SigTC;
		SigTRC = SigTRC;

		if (SigTR < 0) {
			SigTR = 0;
		}
		if (SigTC < 0) {
			SigTC = 0;
		}
		if (SigTRC < 0) {
			SigTRC = 0;
		}

		// F statistics Hillis 2004
		double Nu, De1, De2, De3;
		if (SigTC == 0) {
			df2 = r - 1;
		} else {
			Nu = c * SigTR + r * SigTC + SigTRC;
			Nu = Nu * Nu;
			De1 = c * SigTR + SigTRC;
			De1 = De1 * De1 / (r - 1);
			De2 = r * SigTC + SigTRC;
			De2 = De2 * De2 / (c - 1);
			De3 = SigTRC * SigTRC / (r - 1) / (c - 1);
			df2 = Nu / (De1 + De2 + De3);
			df2 = Nu / (De1);
		}
		Delta = c * SigTR + r * SigTC + SigTRC;
		System.out.println("delta=" + Delta);
		Delta = 2.0 * Delta / r / c;
		System.out.println("delta=" + Delta);
		// double denom=Delta;
		Delta = effSize * effSize / Delta;

		Delta = effSize * effSize / totalVar;
		System.out.println("delta=" + Delta);

		// tStat=Math.sqrt(Delta);
		FisherFDist fdist = new FisherFDist(1, (int) df2);
		CVF = fdist.inverseF(1 - sigLevel);
		double cdftemp = cdfNonCentralF(1, (int) df2, Delta, CVF);
		powerF = 1 - cdftemp;
		System.out.println("delta=" + Delta + " df2=" + df2 + " CVF=" + CVF);
		// compute p-value
		// Delta=obsDiff*obsDiff/denom;
		// pValF = fdist.cdf(1, (int)df2, 5,Delta);

		return powerF;
	}

	public double cdfNonCentralF(int df1, int df2, double delta, double x) {
		double cdf = 0;
		double cdf2 = 0;
		for (int j = 0; j < INFINITY; j++) {
			BetaDist bDist = new BetaDist(df1 / 2.0 + j, df2 / 2.0);
			double tempF = bDist.cdf(df1 / 2.0 + j, df2 / 2.0, PRECISION, df1
					* x / (df2 + df1 * x));
			double sfactor = 1;
			for (int k = j; k > 0; k--) {
				sfactor = sfactor * (0.5 * delta) / (double) k;
			}
			tempF = sfactor * Math.exp(-delta / 2.0) * tempF;
			cdf = cdf + tempF;
			if (tempF > -ZERO && tempF < ZERO) {
				break;
			}
		}
		return cdf;
	}

	public int factorial(int K) {
		int total = 1;
		for (int i = 1; i <= K; i++) {
			total = i * total;
		}
		return total;
	}

	public double ZTest(double[] var, int r, int c, double totalVar) {
		double SigTR = var[0];
		double SigTC = var[1];
		double SigTRC = var[2];

		// print, SigTRC, SigTR, SigTC
		/*
		 * SigTR=SigTR; SigTC=SigTC*c; SigTRC=SigTRC; SigTRC=SigTRC*c;
		 */

		SigTR = SigTR;
		SigTC = SigTC;
		SigTRC = SigTRC;

		if (SigTR < 0) {
			SigTR = 0;
		}
		if (SigTC < 0) {
			SigTC = 0;
		}
		if (SigTRC < 0) {
			SigTRC = 0;
		}
		// double sigma=Math.sqrt(2*(SigTR/r+SigTC/c+SigTRC/(r*c)));
		// double sigma=Math.sqrt((SigTR/r+SigTC/c+SigTRC/(r*c)));
		double sigma = Math.sqrt(totalVar);
		NormalDist centralN = new NormalDist(0, sigma);
		double v = centralN.inverseF(0, sigma, 1 - sigLevel / 2.0);
		NormalDist nonCentralN = new NormalDist(effSize, sigma);
		powerZ = 1 - nonCentralN.cdf(effSize, sigma, v);
		// pValZ = 1-centralN.cdf(0,sigma,v);

		return powerZ;
	}
	/*
	 * public double FTest2011(double[] OR,int r, int c, int rnew, int
	 * cnew,double sig, double eff ) { double varTR =0 ; double H23=0;
	 * if(OR[3]-OR[4] > 0) H23=OR[3]-OR[4]; varTR=OR[2]+OR[1]-OR[5]+H23; double
	 * ratio = (double)c/(double)cnew;
	 * 
	 * double delDen =varTR+ratio*(OR[5]-OR[2]+(r-1.0)*H23); double
	 * Delta=r/2.0*effSize*effSize/delDen; // System.out.println(varTR);
	 * 
	 * double num=delDen*delDen; double den=varTR+ratio*(OR[5]-OR[2]-H23);
	 * double df2=num/(den*den/(r-1)); // System.out.println("df2="+df2+"c="+c);
	 * FisherFDist fdist = new FisherFDist(1,(int)df2); double
	 * Fval=fdist.inverseF(1-sigLevel); double
	 * cdftemp=cdfNonCentralF(1,(int)df2, Delta, Fval); powerF2011=1-cdftemp;
	 * DOF2011=df2; return powerF2011;
	 * 
	 * }
	 */
}