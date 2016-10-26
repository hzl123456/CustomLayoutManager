package cn.xmrk.layoutmanager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvContent = (RecyclerView) findViewById(R.id.rv_content);
        rvContent.setLayoutManager(new CustomLayoutManager(dip2px(250)));
        rvContent.setAdapter(new Adapter());

        findViewById(R.id.btn_smooth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rvContent.smoothScrollToPosition(5);
            }
        });
    }

    /**
     * dp转像素
     *
     * @param dpValue
     * @return
     */
    public final int dip2px(float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
    }


    class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_image, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            int index = (position + 1) % 6;
            int res = 0;
            switch (index) {
                case 0:
                    res = R.mipmap.item1;
                    break;
                case 1:
                    res = R.mipmap.item2;
                    break;
                case 2:
                    res = R.mipmap.item3;
                    break;
                case 3:
                    res = R.mipmap.item4;
                    break;
                case 4:
                    res = R.mipmap.item5;
                    break;
                case 5:
                    res = R.mipmap.item6;
                    break;
            }
            ((MyViewHolder) holder).imageView.setImageResource(res);
            ((MyViewHolder) holder).imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "点击了第" + position + "项目", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return 20;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            RoundImageView imageView;

            public MyViewHolder(View itemView) {
                super(itemView);
                imageView = (RoundImageView) itemView.findViewById(R.id.image);

            }
        }
    }
}
