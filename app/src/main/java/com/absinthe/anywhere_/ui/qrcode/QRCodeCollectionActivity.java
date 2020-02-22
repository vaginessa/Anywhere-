package com.absinthe.anywhere_.ui.qrcode;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.absinthe.anywhere_.BaseActivity;
import com.absinthe.anywhere_.R;
import com.absinthe.anywhere_.adapter.card.QRCollectionAdapter;
import com.absinthe.anywhere_.adapter.manager.WrapContentStaggeredGridLayoutManager;
import com.absinthe.anywhere_.databinding.ActivityQrcodeCollectionBinding;
import com.absinthe.anywhere_.model.OnceTag;
import com.absinthe.anywhere_.model.QRCollection;

import jonathanfinerty.once.Once;

public class QRCodeCollectionActivity extends BaseActivity {
    private ActivityQrcodeCollectionBinding binding;
    private QRCollectionAdapter mAdapter;

    @Override
    protected void setViewBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qrcode_collection);
    }

    @Override
    protected void setToolbar() {
        mToolbar = binding.toolbar.toolbar;
    }

    @Override
    protected void initView() {
        super.initView();

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.QR_COLLECTION_TIP)) {
            ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(this).inflate(
                    R.layout.card_qr_collection_tip, binding.llContainer, false);
            binding.llContainer.addView(viewGroup, 0);
            viewGroup.findViewById(R.id.btn_ok).setOnClickListener(v -> {
                binding.llContainer.removeView(viewGroup);
                Once.markDone(OnceTag.QR_COLLECTION_TIP);
            });
        }

        mAdapter = new QRCollectionAdapter(this);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setLayoutManager(new WrapContentStaggeredGridLayoutManager(2, RecyclerView.VERTICAL));

        binding.srlQrCollection.setRefreshing(true);
        new Handler(Looper.getMainLooper()).post(() -> {
            QRCollection collection = QRCollection.Singleton.INSTANCE.getInstance();
            mAdapter.setItems(collection.getList());
            binding.srlQrCollection.setRefreshing(false);
            binding.srlQrCollection.setEnabled(false);
        });
    }
}
