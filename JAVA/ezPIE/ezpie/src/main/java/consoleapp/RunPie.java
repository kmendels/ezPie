/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package consoleapp;

import java.io.IOException;

import com.fanniemae.ezpie.JobManager;
import com.fanniemae.ezpie.common.DataTable;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-19
 * 
 */

public class RunPie {

	public static void main(String[] args) {
		runJobManager();
	}

	protected static void runJobManager() {
		// String sSettings = "C:\\Developers\\Test\\_Settings.xml";
		String sSettings = "C:\\Developers\\Code\\TestDirectory\\_Settings.xml";
		// String job = "test.xml";
		// String job = "CMFT_Monthly_Scores.xml";
		// String job = "CMFT_Monthly_v2.xml";
		// String job = "HighlightDownload.xml";
		// String job = "ZipTest.xml";
		// String job = "AIP_All_Rules.xml";
		// String job = "GitTest.xml";
		// String job = "GitPIE.xml";
		// String job = "Git_PAM.xml";
		// String job = "SVN_Test.xml";
		// String job = "WireGateway.xml";
		// String job = "AIP_Rules_Criteria.xml";
		// String job = "JoinTest.xml";
		// String job = "ReportTest.xml";
		// String job = "clmDefinition.xml";
		// String job = "clm.xml";
		// String job = "FileDirectory_Test.xml";
		// String job = "AIP_AFP_Export.xml";
		// String job = "Backup_Fnma_Apps.xml";
		// String job = "Monthly_CQA_Report.xml";
		// String job = "REPORT_EFP_YTD.xml";
		// String job = "VersionFile_Test.xml";
		// String job = "Evas_Rescan.xml";
		// String job = "AIP_OnBoard.xml";
		// String job = "SVN_Test.xml";
		// String job = "Rescan_EPV.xml";
		// String job = "Test_Actions.xml";
		// String job = "dbsimple.xml";
		// String job = "SqlExecute.xml";
		// String job = "SMDU.xml";
		// String job = "PEWholeLoanB2B.xml";
		// String job = "SharedTest.xml";
		// String job = "SharedA.xml";
		// String job = "DatabaseBackup.xml";
		// String job = "RescanTest.xml";
		// String job = "ResultSet.xml";
		// String job = "RunCmdError.xml";
		// String job = "LogFilenameTest.xml";
		// String job = "EmptyScanTest.xml";

		// String job = "Add_RescanTables.xml";

		// String job = "Scan_Limiter.xml";

		//String job = "Backup_Tables.xml";

		// String job = "BranchingTest.xml";
		// String job = "AIP_OnBoard.xml";
		// String job = "EPV.xml";
		// String job = "RemoveStarRisk.xml";
		// String job = "BlankDefinition.xml";
		// String job = "If_Test.xml";
		// String job = "Xml_Test.xml";
		// String job = "Snapshot_Date.xml";
		// String job = "RemoveStarRiskRating.xml";
		// String job = "DeleteEmpty.xml";
		// String job = "VerifyJavaFiles.xml";
		// String job = "REST_Test.xml";
		// String job = "GeneralTest.xml";
		// String job = "CreateProjectFiles.xml";
		// String job = "TokenTests.xml";
		// String job = "DbSvnCloneTest.xml";
		// String job = "CreateProjTest2.xml";
		// String job = "DestroyProcess.xml";
		// String job = "_MoveTest.xml";
		
		// String job = "_FindData.xml";
		// String job = "UnionTest.xml";
		// String job = "ExcelTest.xml";
		// String job = "SharepointTest.xml";
		// String job = "ScheduleTest2.xml";
		// String job = "_DataTransforms";
		// String job = "CAST_Daily.xml";
		// String job = "Git.xml";
		// String job = "_Empty.xml";
		// String job = "ListDefinitions";
		// String job = "_Tensorflow";
		// String job = "_TransformDebug";
		// String job = "_Tensorflow_dual";
		// String job ="_CodeRepositories";
		// String job ="_RenameColumn";
		// String job = "TokenTests";
		// String job = "FULL_ETL_TEST";
		// String job="Nexus_RESTxx";
		// String job = "GitCloneTest";
		// String job = "SSL_Test";
		// String job = "EncryptionTest";
		// String job = "iCart";
		// String job = "StoredProcedure";
		// String job = "PythonTest";
		// String job = "iCartTriageResults";
		// String job = "iCartTrainingData";
		// String job = "ChartTest";
		// String job = "RestJDBC";
		// String job = "Jira";
	    String job = "Jenkins";
		String logFilename = null;
		try {
			System.out.println("Initializing PIE JobManager");
			JobManager jobManager = new JobManager(sSettings, job, null);
			logFilename = jobManager.getLogFilename();
			//viewlog(logFilename);
			System.out.println("Running job definition " + job);
			//jobManager.runJob();
			String dataSets = jobManager.getDataJson();
			System.out.println(dataSets);
			System.out.println("Job defintion processing completed.");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		// viewlog(logFilename);
	}

	protected static void viewlog(String logFilename) {
		if (logFilename == null)
			return;
		try {
			Runtime.getRuntime().exec(new String[] { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", logFilename });
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void testDatatable() {
		DataTable dt = new DataTable();
		String[][] aaa = new String[][] { { "Col1", "String" }, { "Col2", "Integer" }, { "Col3", "int" } };
		Object[] myrow = new Object[] { 1, 2 };
		dt.setSchema(aaa);
		dt.addRow(myrow);
		System.out.println("Done");
	}

}
