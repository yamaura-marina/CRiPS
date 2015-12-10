package ronproeditor.ext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import clib.common.filesystem.CDirectory;
import coco.controller.CCCompileErrorKindLoader;
import coco.controller.CCCompileErrorLoader;
import coco.controller.CCMetricsLoader;
import coco.model.CCCompileErrorManager;
import coco.model.CCPathData;
import coco.view.CCMainFrame;
import ppv.app.datamanager.PPProjectSet;
import ppv.app.datamanager.PPRonproPPVLoader;
import ronproeditor.REApplication;

public class RECocoViewerManager {
	private REApplication application;

	/*******************
	 * FILE NAME
	 *******************/
	private static String KINDS_FILE = "ErrorKinds.csv";
	private static String DATA_FILE = "CompileErrorLog.csv";
	private static String ORIGINAL_DATA_FILE = "CompileError.csv"; // ppvから出力されるcsvファイル
	private static String METRICS_FILE = "FileMetrics.csv";

	/********************
	 * PATH NAME
	 ********************/
	private static String PPV_ROOT_DIR = ".ppv";// MyProjects/.ppvフォルダに展開する
	private static String PPV_PROJECTSET_NAME = "hoge";// projectset名
	private static String PPV_TMP_DIR = "tmp";
	private static String PPV_DATA_DIR = "ppv.data/data";
	private static String COCOVIEWER_DIR_NAME = "cocoviewer";
	private String ppvRootPath = "";
	
	/********************
	 * COCOVIEWER DATA
	 *******************/
	private CCPathData cocoPathData = new CCPathData();
	private CCCompileErrorManager manager;
	private static CCMainFrame cocoWindow;

	public RECocoViewerManager(REApplication application) {
		this.application = application;
		this.ppvRootPath = application.getSourceManager().getCRootDirectory()
				.findOrCreateDirectory(PPV_ROOT_DIR).getAbsolutePath()
				.toString()
				+ "/";
	}

	public void openCocoViewer(PPProjectSet ppProjectSet) {
		manager = new CCCompileErrorManager();

		loadData();
		setPath(ppProjectSet); // ppProjectSetなどの情報があるので，消さないでおくこと

		// start cocoviewer
		cocoWindow = new CCMainFrame(manager);
		cocoWindow.setVisible(true);
	}

	private void loadData() {
		// error check
		dataFileCheck();
		
		CCCompileErrorKindLoader kindLoader = new CCCompileErrorKindLoader(
				manager);
		kindLoader.load(application.getExtensionDirectory()
				.findDirectory(COCOVIEWER_DIR_NAME).findFile(KINDS_FILE).getAbsolutePath().toString());

		CCCompileErrorLoader errorLoader = new CCCompileErrorLoader(manager);
		errorLoader.load(ppvRootPath + DATA_FILE);

		CCMetricsLoader metricsLoader = new CCMetricsLoader(manager);
		metricsLoader.load(ppvRootPath + METRICS_FILE);
	}
	
	private void dataFileCheck() {
		ArrayList<String> filepaths = new ArrayList<String>();
		filepaths.add(ppvRootPath + DATA_FILE);
		filepaths.add(ppvRootPath + METRICS_FILE);
	
		if(!existFilePath(filepaths)) {
			JOptionPane.showMessageDialog(application.getFrame(), "Compile Error修正データが見つかりません", "Compile Error Cash Not Found", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private boolean existFilePath(ArrayList<String> filepaths) {
		for(String filepath : filepaths) {
			File file = new File(filepath);
			if(!file.exists()) {
				return false;
			}
		}
		return true;		
	}

	private void setPath(PPProjectSet ppProjectSet) {
		CDirectory ppvRoot = application.getSourceManager().getCRootDirectory()
				.findOrCreateDirectory(PPV_ROOT_DIR);
		CDirectory libDir = application.getLibraryManager().getDir();
		manager.setBaseDir(ppvRoot);
		manager.setLibDir(libDir);
		manager.setPPProjectSet(ppProjectSet);
		
		// File Path set
		{
			String filepath = application.getExtensionDirectory()
					.findDirectory(COCOVIEWER_DIR_NAME).findFile(KINDS_FILE).getAbsolutePath().toString();
			cocoPathData.setKindsFilePath(filepath);
		}
		
		{
			String filepath = application.getSourceManager().getCRootDirectory()
					.findOrCreateDirectory(PPV_ROOT_DIR).getAbsolutePath()
					.toString()
					+ "/" + DATA_FILE;
			cocoPathData.setDataFilePath(filepath);
		}

		{
			String filepath = application.getSourceManager().getCRootDirectory()
					.findOrCreateDirectory(PPV_ROOT_DIR).getAbsolutePath()
					.toString()
					+ "/" + ORIGINAL_DATA_FILE;
			cocoPathData.setOriginalDataFilePath(filepath);
		}

		{
			String filepath = application.getSourceManager().getCRootDirectory()
					.findOrCreateDirectory(PPV_ROOT_DIR).getAbsolutePath()
					.toString()
					+ "/" + METRICS_FILE;
			cocoPathData.setMetricsFilePath(filepath);
		}


		// Dirs Path set
		{
			CDirectory dir = application.getSourceManager()
					.getCRootDirectory().findOrCreateDirectory(PPV_ROOT_DIR);
			cocoPathData.setPPVRootDir(dir);
		}

		{
			CDirectory dir = application.getLibraryManager().getDir();
			cocoPathData.setPPVLibDir(dir);
		}
		
		{
			CDirectory dir = application.getSourceManager()
					.getCRootDirectory().findOrCreateDirectory(PPV_ROOT_DIR).findOrCreateDirectory(PPV_TMP_DIR);
			cocoPathData.setPPVTempDir(dir);
		}
		
		{
			CDirectory dir = application.getSourceManager()
					.getCRootDirectory().findOrCreateDirectory(PPV_ROOT_DIR).findOrCreateDirectory(PPV_DATA_DIR)
					.findOrCreateDirectory(PPV_PROJECTSET_NAME);
			cocoPathData.setPPVProjectSetName(dir);
		}
		
		manager.setCCPathData(cocoPathData);
		
		// Projects set
		{
			List<CDirectory> projects = application.getSourceManager()
					.getAllProjects();
			manager.setProjects(projects);	
		}
		
		// Loader set
		{
			manager.setPPVLoader(new PPRonproPPVLoader());
		}
	}
}
