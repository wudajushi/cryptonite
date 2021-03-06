package csh.cryptonite.storage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.DirectorySettings;
import csh.cryptonite.ProgressDialogFragment;
import csh.cryptonite.R;
import csh.cryptonite.database.Volume;

public abstract class StorageFragment extends SherlockFragment {

    public TextView tv;
    
    protected Cryptonite mAct;
    protected Button buttonDecryptOrForget, buttonBrowseDecrypted,
        buttonCreate, buttonSaveLoad;
    protected int idLayout, idTvVersion, idBtnDecrypt, idTxtDecrypt, idBtnBrowseDecrypted,
        idBtnSaveLoad, idBtnCreate;
    protected int storageType;
    protected int opMode;
    protected int dialogMode, dialogModeDefault;
    protected View mView;
    
    public StorageFragment() {
        super();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(idLayout, container, false);

        mAct = (Cryptonite)getActivity();
        
        tv = (TextView)mView.findViewById(idTvVersion);
        
        /* Decrypt EncFS volume */
        buttonDecryptOrForget = (Button)mView.findViewById(idBtnDecrypt);
        buttonDecryptOrForget.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()) {
                        mAct.cleanUpDecrypted();
                        updateDecryptButtons();
                    } else {
                        StorageManager.INSTANCE.initEncFSStorage(mAct, storageType);
                        mAct.opMode = opMode;
                        mAct.currentDialogLabel = getString(R.string.select_enc);
                        mAct.currentDialogButtonLabel = getString(
                                R.string.select_enc_short);
                        mAct.currentDialogMode = dialogMode;
                        openEncFSVolume();
                    }
                }});

        /* Browse decrypted volume */
        buttonBrowseDecrypted = (Button)mView.findViewById(idBtnBrowseDecrypted);
        buttonBrowseDecrypted.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.browseEncFS(DirectorySettings.INSTANCE.currentBrowsePath, 
                            DirectorySettings.INSTANCE.currentBrowseStartPath);
                }});
        
        /* Save as default */
        buttonSaveLoad = (Button)mView.findViewById(idBtnSaveLoad);
        buttonSaveLoad.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess()) {
                        mAct.saveDefault(storageType, Volume.VIRTUAL);
                    } else {
                        final Volume volume = mAct.restoreDefault(storageType, Volume.VIRTUAL);
                        StorageManager.INSTANCE.initEncFSStorage(mAct, storageType);
                        ProgressDialogFragment.showDialog(mAct, R.string.default_searching, "searchDefault");
                        new Thread(new Runnable(){
                            public void run(){
                                final boolean defaultExists = StorageManager.INSTANCE.getEncFSStorage().exists(volume.getSource());
                                mAct.runOnUiThread(new Runnable(){
                                    public void run() {
                                        ProgressDialogFragment.dismissDialog(mAct, "searchDefault");
                                        if (defaultExists) {
                                            mAct.opMode = opMode;
                                            mAct.currentDialogLabel = getString(R.string.select_enc);
                                            mAct.currentDialogButtonLabel = getString(
                                                    R.string.select_enc_short);
                                            mAct.currentDialogMode = dialogModeDefault;
                                            openEncFSVolumeDefault(volume);
                                        } else {
                                            Toast.makeText(mAct, 
                                                    getString(R.string.default_missing) + " (" + volume.getSource() + ")", 
                                                    Toast.LENGTH_LONG).show();   
                                        }
                                    }
                                });
                            }
                        }).start();                        
                    }
                }});

        /* Create EncFS volume */
        buttonCreate = (Button)mView.findViewById(idBtnCreate);
        buttonCreate.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAct.createEncFS(storageType == Storage.STOR_DROPBOX);
                }});

        return mView;
    }
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tv.setText(mAct.textOut);
        updateDecryptButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDecryptButtons();
        if (tv != null) {
            tv.setText(mAct.textOut);
            tv.invalidate();
        }
    }
    
    abstract protected void openEncFSVolume();
    abstract protected void openEncFSVolumeDefault(Volume volume);

    public void updateDecryptButtons() {
        if (buttonDecryptOrForget == null || buttonBrowseDecrypted == null ||
                buttonCreate == null || buttonSaveLoad == null) 
        {
            return;
        }

        boolean volumeLoaded = (Cryptonite.jniVolumeLoaded() == Cryptonite.jniSuccess());

        if (volumeLoaded) {
            buttonDecryptOrForget.setText(R.string.forget_decryption);
            buttonDecryptOrForget.setEnabled(true);
        } else {
            buttonDecryptOrForget.setText(idTxtDecrypt);
            buttonDecryptOrForget.setEnabled(true);
        }
        buttonBrowseDecrypted.setEnabled(volumeLoaded &&
                StorageManager.INSTANCE.getEncFSStorageType() == storageType);
        if (volumeLoaded && StorageManager.INSTANCE.getEncFSStorageType() == storageType) {
            buttonSaveLoad.setText(R.string.default_save);
            buttonSaveLoad.setEnabled(true);
        } else if (!volumeLoaded) {
            buttonSaveLoad.setText(R.string.default_restore);
            /* Is there any saved volume at all? */
            buttonSaveLoad.setEnabled(mAct.hasDefault(storageType, Volume.VIRTUAL));
        } else {
            buttonSaveLoad.setEnabled(false);
        }
        buttonCreate.setEnabled(!volumeLoaded);
    }
    
}
