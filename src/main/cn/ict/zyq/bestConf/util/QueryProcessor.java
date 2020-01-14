package cn.ict.zyq.bestConf.util;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryProcessor {
    static ArrayList<String> operators = new ArrayList<String>(
            Arrays.asList("Filter", "Single-row index lookup", "Table scan", "Inner hash join", "Nested loop inner join",
                          "Materialize CTE", "Index lookup"));
    public static HashMap<String, ArrayList<Double>> process(String queryPlan) {
        String[] lines = queryPlan.split("->");
        HashMap<String, ArrayList<Double>> costMap = new HashMap<String, ArrayList<Double>>();
//        HashMap<String, Integer> perfMetrics = new HashMap<String, Integer>();
//
//        String last_line = lines[lines.length - 1];
//        String[] metrics = last_line.split("\n");
//        lines[lines.length - 1] = metrics[0];

        for (String i:lines) {
            if (i.equals("")) {
                continue;
            } else if (i.contains("temporary")) {
                continue;
            }

            String operator = "";
            if (i.contains("Filter")) {
                operator = "Filter";
            } else if (i.contains("Table scan")) {
                operator = "Table scan";
            } else if (i.contains("Single-row index lookup")) {
                operator = "Single-row index lookup";
            } else if (i.contains("Inner hash join")) {
                operator = "Inner hash join";
            } else if (i.contains("Nested loop inner join")) {
                operator = "Nested loop inner join";
            } else if (i.contains("Index lookup")) {
                operator = "Index lookup";
//            } else if (i.contains("Aggregate")) {
//                operator = "Aggregate";
//            }
            } else if (i.contains("Sort")) {
                operator = "Sort";
            } else {
                continue;
            }

            Pattern p = Pattern.compile("\\((.*?)\\)");
            Matcher m = p.matcher(i);
            String costData = "";
            while(m.find()) {
                costData = m.group(1);
            }
            ArrayList<Double> costs = new ArrayList<Double>();

            for (String c : costData.split(" ")) {
                String data = c.split("=")[1];
                costs.add(Double.parseDouble(data));
            }
            System.out.println("new here");
            if (!operator.equals("")) {
                if (!costMap.keySet().contains(operator)) {
                    costs.add(1.0);
                    costMap.put(operator, costs);
                } else {
                    ArrayList<Double> accumulated = costMap.get(operator);
                    accumulated.set(0, accumulated.get(0) + costs.get(0));
                    accumulated.set(1, accumulated.get(1) + costs.get(1));
                    accumulated.set(2, accumulated.get(2) + 1);
                    costMap.put(operator, accumulated);
                }
            }
            System.out.println("end here");
        }
//
//        for (String i : metrics) {
//            String[] pair = i.split("\t");
//            perfMetrics.put(pair[0], Integer.parseInt(pair[1]));
//        }
        for (String op : costMap.keySet()) {
            System.out.print(op);
            System.out.print(" ");
            for (Double data : costMap.get(op)) {
                System.out.print(data);
                System.out.print(" ");
            }
            System.out.println(" ");
        }
        return costMap;
    }

    public static HashMap<String, Double> metaProcessPerformance(String[] queryPlans, int type) {
        if (type == 1) {
            return processPeformance(queryPlans[0]);
        } else {
            return processPeformance(queryPlans[0]);
        }
    }

    public static HashMap<String, Double> processPeformance(String queryPlan) {

        String firstGroup = queryPlan.split("EXPLAIN\n")[0];
        String secondGroup = queryPlan.split("EXPLAIN\n")[1].split("Variable_name\tValue\n")[1];
        String[] lines = queryPlan.split("EXPLAIN\n")[1].split("Variable_name\tValue\n")[0].split("->");

        HashMap<String, ArrayList<Double>> costMap = new HashMap<String, ArrayList<Double>>();

        HashMap<String, Double> perfMetrics = new HashMap<String, Double>();
        HashMap<String, Double> firstPerfMetrics = new HashMap<String, Double>();
        HashMap<String, Double> secondPerfMetrics = new HashMap<String, Double>();

        String[] firstMetrics = firstGroup.split("\n");
        String[] secondMetrics = secondGroup.split("\n");

        int totalRowAccessed = 0;
        for (String i:lines) {
            if (i.equals("")) {
                continue;
            } else if (i.contains("temporary")) {
                continue;
            }

            String operator = "";
            if (i.contains("Filter")) {
                operator = "Filter";
            } else if (i.contains("Table scan")) {
                operator = "Table scan";
            } else if (i.contains("Single-row index lookup")) {
                operator = "Single-row index lookup";
            } else if (i.contains("Inner hash join")) {
                operator = "Inner hash join";
            } else if (i.contains("Nested loop inner join")) {
                operator = "Nested loop inner join";
            } else if (i.contains("Index lookup")) {
                operator = "Index lookup";
//            } else if (i.contains("Aggregate")) {
//                operator = "Aggregate";
//            }
            } else if (i.contains("Sort")) {
                operator = "Sort";
            } else {
                continue;
            }

            Pattern p = Pattern.compile("\\((.*?)\\)");
            Matcher m = p.matcher(i);
            String costData = "";
            while(m.find()) {
                costData = m.group(1);
            }
            ArrayList<Double> costs = new ArrayList<Double>();

            for (String c : costData.split(" ")) {
                if (c.equals("actual")) {
                    continue;
                }
                if (c.split("=")[0].equals("time")) {
                    continue;
                }
                if (c.split("=")[0].equals("rows")) {
                    totalRowAccessed += Integer.parseInt(c.split("=")[1]);
                }
                String data = c.split("=")[1];
                costs.add(Double.parseDouble(data));
            }
//            System.out.println("new here");
            if (!operator.equals("")) {
                if (!costMap.keySet().contains(operator)) {
                    costs.add(1.0);
                    costMap.put(operator, costs);
//                    System.out.println(operator);
//                    System.out.println(costs);
                } else {
                    ArrayList<Double> accumulated = costMap.get(operator);
                    accumulated.set(0, accumulated.get(0) + costs.get(0));
                    accumulated.set(1, accumulated.get(1) + costs.get(1));
                    accumulated.set(2, accumulated.get(2) + 1);
                    costMap.put(operator, accumulated);
//                    System.out.println(operator);
//                    System.out.println(accumulated);
                }
            }
//            System.out.println("end here");
        }
//        System.out.println("Total rows:");
//        System.out.println(totalRowAccessed);


        Pattern p = Pattern.compile("\\((.*?)\\)");
        Matcher m = p.matcher(lines[1]);
        String total = "";
        while(m.find()) {
            total = m.group(1);
        }
        String s = total.split("=")[1].split("\\.\\.")[1].split(" ")[0];
        Double totalTime = Double.parseDouble(s);
        perfMetrics.put("totalTime", totalTime);

        System.out.println("totalTime");
        System.out.println(perfMetrics.get("totalTime"));

        for (int i = 2; i < firstMetrics.length; i++) {
            String data = firstMetrics[i];
            String[] pair = data.split("\t");
            try {
                firstPerfMetrics.put(pair[0], Double.parseDouble(pair[1]));
//                perfMetrics.put(pair[0], Double.parseDouble(pair[1]));
            } catch (Exception e) {
                continue;
            }
        }

        for (int i = 2; i < secondMetrics.length; i++) {
            String data = secondMetrics[i];
            String[] pair = data.split("\t");
            try {
                secondPerfMetrics.put(pair[0], Double.parseDouble(pair[1]));
                perfMetrics.put(pair[0], Double.parseDouble(pair[1]));
            } catch (Exception e) {
                continue;
            }
        }
        String stateString = "Opened_tables\n" +
                "Threads_created\n" +
                "Created_tmp_files\n" +
                "Created_tmp_tables\n" +
                "Opened_table_definitions\n" +
                "Created_tmp_disk_tables\n" +
                "Innodb_buffer_pool_pages_data\n" +
                "Innodb_buffer_pool_bytes_data\n" +
                "Innodb_buffer_pool_pages_dirty\n" +
                "Innodb_buffer_pool_bytes_dirty\n" +
                "Innodb_buffer_pool_pages_flushed\n" +
                "Innodb_buffer_pool_pages_free\n" +
                "Innodb_buffer_pool_pages_misc\n" +
                "Innodb_buffer_pool_pages_total\n" +
                "Innodb_buffer_pool_read_ahead_rnd\n" +
                "Innodb_buffer_pool_read_ahead\n" +
                "Innodb_buffer_pool_read_ahead_evicted\n" +
                "Innodb_buffer_pool_read_requests\n" +
                "Innodb_buffer_pool_reads\n" +
                "Innodb_buffer_pool_wait_free\n" +
                "Innodb_buffer_pool_write_requests\n" +
                "Innodb_data_fsyncs\n" +
                "Innodb_data_pending_fsyncs\n" +
                "Innodb_data_pending_reads\n" +
                "Innodb_data_pending_writes\n" +
                "Innodb_data_read\n" +
                "Innodb_data_reads\n" +
                "Innodb_data_writes\n" +
                "Innodb_data_written\n" +
                "Innodb_dblwr_pages_written\n" +
                "Innodb_dblwr_writes\n" +
                "Innodb_log_waits\n" +
                "Innodb_log_write_requests\n" +
                "Innodb_log_writes\n" +
                "Innodb_os_log_fsyncs\n" +
                "Innodb_os_log_pending_fsyncs\n" +
                "Innodb_os_log_pending_writes\n" +
                "Innodb_os_log_written\n" +
                "Innodb_page_size\n" +
                "Innodb_pages_created\n" +
                "Innodb_pages_read\n" +
                "Innodb_pages_written\n" +
                "Innodb_row_lock_current_waits\n" +
                "Innodb_row_lock_time\n" +
                "Innodb_row_lock_time_avg\n" +
                "Innodb_row_lock_time_max\n" +
                "Innodb_row_lock_waits\n" +
                "Innodb_rows_deleted\n" +
                "Innodb_rows_inserted\n" +
                "Innodb_rows_read\n" +
                "Innodb_rows_updated\n" +
                "Innodb_num_open_files\n" +
                "Innodb_truncated_status_writes\n" +
                "Innodb_undo_tablespaces_total\n" +
                "Innodb_undo_tablespaces_implicit\n" +
                "Innodb_undo_tablespaces_explicit\n" +
                "Innodb_undo_tablespaces_active";

        String[] stateKeywords = stateString.split("\n");
        ArrayList<String> environment = new ArrayList<String>();

        for (String ss : stateKeywords) {
            environment.add(ss);
        }

//        environment.add("Innodb_rows_inserted");
//        environment.add("Innodb_rows_read");
//        environment.add("Innodb_rows_updated");
//        environment.add("Innodb_page_size");
//        environment.add("Innodb_pages_created");
//        environment.add("Innodb_pages_read");
//        environment.add("Innodb_pages_written");

        ArrayList<String> reward = new ArrayList<String>();
        reward.add("Innodb_data_reads");
        reward.add("Innodb_data_read");
        reward.add("Innodb_data_writes");
        reward.add("Innodb_data_written");

        HashMap<String, Double> returnMetrics = new HashMap<String, Double>();

        for (String e : environment) {
            returnMetrics.put(e, perfMetrics.get(e));
        }
        Double latency = perfMetrics.get("totalTime");
//        + secondPerfMetrics.get("Innodb_data_reads") - firstPerfMetrics.get("Innodb_data_reads")
//                + secondPerfMetrics.get("Innodb_data_writes") - firstPerfMetrics.get("Innodb_data_writes");

        Double thoughput = (secondPerfMetrics.get("Questions") - firstPerfMetrics.get("Questions")) / (secondPerfMetrics.get("Uptime") - firstPerfMetrics.get("Uptime"));
        System.out.println("thoughput");
        System.out.println(thoughput);
        returnMetrics.put("totalTime", latency);

        return returnMetrics;
    }

    public static void main(String[] args) {
        //String queryPlanString = args[0];
        String queryPlanString = "EXPLAIN\n" +
                "-> Limit: 100 row(s)\\n    -> Sort: <temporary>.i_category, <temporary>.i_class, <temporary>.i_item_id, <temporary>.i_item_desc, ((sum(web_sales.ws_ext_sales_price) * 100) / sum(sum(web_sales.ws_ext_sales_price)) OVER (PARTITION BY item.i_class ) )\\n        -> Table scan on <temporary>\\n            -> Materialize\\n                -> Window aggregate with buffering\\n                    -> Sort: <temporary>.i_class\\n                        -> Table scan on <temporary>\\n                            -> Aggregate using temporary table\\n                                -> Nested loop inner join  (cost=381633.79 rows=42760)\\n                                    -> Nested loop inner join  (cost=51383.13 rows=384879)\\n                                        -> Filter: (item.i_category in ('Jewelry','Sports','Books'))  (cost=3244.56 rows=7559)\\n                                            -> Table scan on item  (cost=3244.56 rows=25196)\\n                                        -> Filter: (web_sales.ws_sold_date_sk is not null)  (cost=1.28 rows=51)\\n                                            -> Index lookup on web_sales using PRIMARY (ws_item_sk=item.i_item_sk)  (cost=1.28 rows=51)\\n                                    -> Filter: (date_dim.d_date between <cache>(cast('2001-01-12' as date)) and <cache>(('2001-01-12' + interval 30 day)))  (cost=0.76 rows=0)\\n                                        -> Single-row index lookup on date_dim using PRIMARY (d_date_sk=web_sales.ws_sold_date_sk)  (cost=0.76 rows=1)\\n\n";
        String performString = "Variable_name\tValue\n" +
                "Aborted_clients\t0\n" +
                "Aborted_connects\t0\n" +
                "Acl_cache_items_count\t0\n" +
                "Binlog_cache_disk_use\t0\n" +
                "Binlog_cache_use\t0\n" +
                "Binlog_stmt_cache_disk_use\t0\n" +
                "Binlog_stmt_cache_use\t0\n" +
                "Bytes_received\t268\n" +
                "Bytes_sent\t188\n" +
                "Caching_sha2_password_rsa_public_key\t-----BEGIN PUBLIC KEY-----\\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsnW6k9tEqLsHbKPf+lfP\\nNY5ZNLtLuzS9rxCVDwF3eI3Mi1WdfLUbKvkiHlM2BtDCwYwryCkEcRlXlvfStVam\\nRDVWhDauzFdFmAmeVMp4leGQiJ3JjWIaRLXlYRddfGBNG/7qkh2y8nm3gP5vgVa7\\nznhmlyEW/guH5Fiso3JByysvP68H3ptxAiJyykxl9GHBGxDVmSvTvveXQZi+xz5w\\nqHZIKHC5TRPKZvm3vFoDtoeIbt7VO7t0IC5NtbT31UM2eZBjvczLr332wc7ZDaqW\\n/AX482PpVGjAVD5TYY1lKIcYgJ8l2WYxO8qOZlbp4WDU9Ui7EuKuDoVYNtq4yiI4\\nhwIDAQAB\\n-----END PUBLIC KEY-----\\n\n" +
                "Com_admin_commands\t0\n" +
                "Com_assign_to_keycache\t0\n" +
                "Com_alter_db\t0\n" +
                "Com_alter_event\t0\n" +
                "Com_alter_function\t0\n" +
                "Com_alter_instance\t0\n" +
                "Com_alter_procedure\t0\n" +
                "Com_alter_resource_group\t0\n" +
                "Com_alter_server\t0\n" +
                "Com_alter_table\t0\n" +
                "Com_alter_tablespace\t0\n" +
                "Com_alter_user\t0\n" +
                "Com_alter_user_default_role\t0\n" +
                "Com_analyze\t0\n" +
                "Com_begin\t0\n" +
                "Com_binlog\t0\n" +
                "Com_call_procedure\t0\n" +
                "Com_change_db\t0\n" +
                "Com_change_master\t0\n" +
                "Com_change_repl_filter\t0\n" +
                "Com_check\t0\n" +
                "Com_checksum\t0\n" +
                "Com_clone\t0\n" +
                "Com_commit\t0\n" +
                "Com_create_db\t0\n" +
                "Com_create_event\t0\n" +
                "Com_create_function\t0\n" +
                "Com_create_index\t0\n" +
                "Com_create_procedure\t0\n" +
                "Com_create_role\t0\n" +
                "Com_create_server\t0\n" +
                "Com_create_table\t0\n" +
                "Com_create_resource_group\t0\n" +
                "Com_create_trigger\t0\n" +
                "Com_create_udf\t0\n" +
                "Com_create_user\t0\n" +
                "Com_create_view\t0\n" +
                "Com_create_spatial_reference_system\t0\n" +
                "Com_dealloc_sql\t0\n" +
                "Com_delete\t0\n" +
                "Com_delete_multi\t0\n" +
                "Com_do\t0\n" +
                "Com_drop_db\t0\n" +
                "Com_drop_event\t0\n" +
                "Com_drop_function\t0\n" +
                "Com_drop_index\t0\n" +
                "Com_drop_procedure\t0\n" +
                "Com_drop_resource_group\t0\n" +
                "Com_drop_role\t0\n" +
                "Com_drop_server\t0\n" +
                "Com_drop_spatial_reference_system\t0\n" +
                "Com_drop_table\t0\n" +
                "Com_drop_trigger\t0\n" +
                "Com_drop_user\t0\n" +
                "Com_drop_view\t0\n" +
                "Com_empty_query\t0\n" +
                "Com_execute_sql\t0\n" +
                "Com_explain_other\t0\n" +
                "Com_flush\t0\n" +
                "Com_get_diagnostics\t0\n" +
                "Com_grant\t0\n" +
                "Com_grant_roles\t0\n" +
                "Com_ha_close\t0\n" +
                "Com_ha_open\t0\n" +
                "Com_ha_read\t0\n" +
                "Com_help\t0\n" +
                "Com_import\t0\n" +
                "Com_insert\t0\n" +
                "Com_insert_select\t0\n" +
                "Com_install_component\t0\n" +
                "Com_install_plugin\t0\n" +
                "Com_kill\t0\n" +
                "Com_load\t0\n" +
                "Com_lock_instance\t0\n" +
                "Com_lock_tables\t0\n" +
                "Com_optimize\t0\n" +
                "Com_preload_keys\t0\n" +
                "Com_prepare_sql\t0\n" +
                "Com_purge\t0\n" +
                "Com_purge_before_date\t0\n" +
                "Com_release_savepoint\t0\n" +
                "Com_rename_table\t0\n" +
                "Com_rename_user\t0\n" +
                "Com_repair\t0\n" +
                "Com_replace\t0\n" +
                "Com_replace_select\t0\n" +
                "Com_reset\t0\n" +
                "Com_resignal\t0\n" +
                "Com_restart\t0\n" +
                "Com_revoke\t0\n" +
                "Com_revoke_all\t0\n" +
                "Com_revoke_roles\t0\n" +
                "Com_rollback\t0\n" +
                "Com_rollback_to_savepoint\t0\n" +
                "Com_savepoint\t0\n" +
                "Com_select\t1\n" +
                "Com_set_option\t0\n" +
                "Com_set_password\t0\n" +
                "Com_set_resource_group\t0\n" +
                "Com_set_role\t0\n" +
                "Com_signal\t0\n" +
                "Com_show_binlog_events\t0\n" +
                "Com_show_binlogs\t0\n" +
                "Com_show_charsets\t0\n" +
                "Com_show_collations\t0\n" +
                "Com_show_create_db\t0\n" +
                "Com_show_create_event\t0\n" +
                "Com_show_create_func\t0\n" +
                "Com_show_create_proc\t0\n" +
                "Com_show_create_table\t0\n" +
                "Com_show_create_trigger\t0\n" +
                "Com_show_databases\t0\n" +
                "Com_show_engine_logs\t0\n" +
                "Com_show_engine_mutex\t0\n" +
                "Com_show_engine_status\t0\n" +
                "Com_show_events\t0\n" +
                "Com_show_errors\t0\n" +
                "Com_show_fields\t0\n" +
                "Com_show_function_code\t0\n" +
                "Com_show_function_status\t0\n" +
                "Com_show_grants\t0\n" +
                "Com_show_keys\t0\n" +
                "Com_show_master_status\t0\n" +
                "Com_show_open_tables\t0\n" +
                "Com_show_plugins\t0\n" +
                "Com_show_privileges\t0\n" +
                "Com_show_procedure_code\t0\n" +
                "Com_show_procedure_status\t0\n" +
                "Com_show_processlist\t0\n" +
                "Com_show_profile\t0\n" +
                "Com_show_profiles\t0\n" +
                "Com_show_relaylog_events\t0\n" +
                "Com_show_slave_hosts\t0\n" +
                "Com_show_slave_status\t0\n" +
                "Com_show_status\t1\n" +
                "Com_show_storage_engines\t0\n" +
                "Com_show_table_status\t0\n" +
                "Com_show_tables\t0\n" +
                "Com_show_triggers\t0\n" +
                "Com_show_variables\t0\n" +
                "Com_show_warnings\t0\n" +
                "Com_show_create_user\t0\n" +
                "Com_shutdown\t0\n" +
                "Com_slave_start\t0\n" +
                "Com_slave_stop\t0\n" +
                "Com_group_replication_start\t0\n" +
                "Com_group_replication_stop\t0\n" +
                "Com_stmt_execute\t0\n" +
                "Com_stmt_close\t0\n" +
                "Com_stmt_fetch\t0\n" +
                "Com_stmt_prepare\t0\n" +
                "Com_stmt_reset\t0\n" +
                "Com_stmt_send_long_data\t0\n" +
                "Com_truncate\t0\n" +
                "Com_uninstall_component\t0\n" +
                "Com_uninstall_plugin\t0\n" +
                "Com_unlock_instance\t0\n" +
                "Com_unlock_tables\t0\n" +
                "Com_update\t0\n" +
                "Com_update_multi\t0\n" +
                "Com_xa_commit\t0\n" +
                "Com_xa_end\t0\n" +
                "Com_xa_prepare\t0\n" +
                "Com_xa_recover\t0\n" +
                "Com_xa_rollback\t0\n" +
                "Com_xa_start\t0\n" +
                "Com_stmt_reprepare\t0\n" +
                "Compression\tOFF\n" +
                "Compression_algorithm\t\n" +
                "Compression_level\t0\n" +
                "Connection_errors_accept\t0\n" +
                "Connection_errors_internal\t0\n" +
                "Connection_errors_max_connections\t0\n" +
                "Connection_errors_peer_address\t0\n" +
                "Connection_errors_select\t0\n" +
                "Connection_errors_tcpwrap\t0\n" +
                "Connections\t54\n" +
                "Created_tmp_disk_tables\t0\n" +
                "Created_tmp_files\t21\n" +
                "Created_tmp_tables\t0\n" +
                "Current_tls_ca\tca.pem\n" +
                "Current_tls_capath\t\n" +
                "Current_tls_cert\tserver-cert.pem\n" +
                "Current_tls_cipher\t\n" +
                "Current_tls_ciphersuites\t\n" +
                "Current_tls_crl\t\n" +
                "Current_tls_crlpath\t\n" +
                "Current_tls_key\tserver-key.pem\n" +
                "Current_tls_version\tTLSv1,TLSv1.1,TLSv1.2,TLSv1.3\n" +
                "Delayed_errors\t0\n" +
                "Delayed_insert_threads\t0\n" +
                "Delayed_writes\t0\n" +
                "Flush_commands\t3\n" +
                "Handler_commit\t0\n" +
                "Handler_delete\t0\n" +
                "Handler_discover\t0\n" +
                "Handler_external_lock\t0\n" +
                "Handler_mrr_init\t0\n" +
                "Handler_prepare\t0\n" +
                "Handler_read_first\t0\n" +
                "Handler_read_key\t0\n" +
                "Handler_read_last\t0\n" +
                "Handler_read_next\t0\n" +
                "Handler_read_prev\t0\n" +
                "Handler_read_rnd\t0\n" +
                "Handler_read_rnd_next\t0\n" +
                "Handler_rollback\t0\n" +
                "Handler_savepoint\t0\n" +
                "Handler_savepoint_rollback\t0\n" +
                "Handler_update\t0\n" +
                "Handler_write\t0\n" +
                "Innodb_buffer_pool_dump_status\tDumping of buffer pool not started\n" +
                "Innodb_buffer_pool_load_status\tBuffer pool(s) load completed at 191217 20:00:26\n" +
                "Innodb_buffer_pool_resize_status\t\n" +
                "Innodb_buffer_pool_pages_data\t7734\n" +
                "Innodb_buffer_pool_bytes_data\t126713856\n" +
                "Innodb_buffer_pool_pages_dirty\t0\n" +
                "Innodb_buffer_pool_bytes_dirty\t0\n" +
                "Innodb_buffer_pool_pages_flushed\t152\n" +
                "Innodb_buffer_pool_pages_free\t0\n" +
                "Innodb_buffer_pool_pages_misc\t458\n" +
                "Innodb_buffer_pool_pages_total\t8192\n" +
                "Innodb_buffer_pool_read_ahead_rnd\t0\n" +
                "Innodb_buffer_pool_read_ahead\t28289\n" +
                "Innodb_buffer_pool_read_ahead_evicted\t1012\n" +
                "Innodb_buffer_pool_read_requests\t67882132\n" +
                "Innodb_buffer_pool_reads\t945275\n" +
                "Innodb_buffer_pool_wait_free\t0\n" +
                "Innodb_buffer_pool_write_requests\t1640\n" +
                "Innodb_data_fsyncs\t61\n" +
                "Innodb_data_pending_fsyncs\t0\n" +
                "Innodb_data_pending_reads\t1\n" +
                "Innodb_data_pending_writes\t0\n" +
                "Innodb_data_read\t15950959616\n" +
                "Innodb_data_reads\t973616\n" +
                "Innodb_data_writes\t231\n" +
                "Innodb_data_written\t3057664\n" +
                "Innodb_dblwr_pages_written\t21\n" +
                "Innodb_dblwr_writes\t8\n" +
                "Innodb_log_waits\t0\n" +
                "Innodb_log_write_requests\t647\n" +
                "Innodb_log_writes\t20\n" +
                "Innodb_os_log_fsyncs\t21\n" +
                "Innodb_os_log_pending_fsyncs\t0\n" +
                "Innodb_os_log_pending_writes\t0\n" +
                "Innodb_os_log_written\t36352\n" +
                "Innodb_page_size\t16384\n" +
                "Innodb_pages_created\t142\n" +
                "Innodb_pages_read\t973564\n" +
                "Innodb_pages_written\t162\n" +
                "Innodb_row_lock_current_waits\t0\n" +
                "Innodb_row_lock_time\t0\n" +
                "Innodb_row_lock_time_avg\t0\n" +
                "Innodb_row_lock_time_max\t0\n" +
                "Innodb_row_lock_waits\t0\n" +
                "Innodb_rows_deleted\t0\n" +
                "Innodb_rows_inserted\t0\n" +
                "Innodb_rows_read\t103323401\n" +
                "Innodb_rows_updated\t315\n" +
                "Innodb_num_open_files\t31\n" +
                "Innodb_truncated_status_writes\t0\n" +
                "Innodb_undo_tablespaces_total\t2\n" +
                "Innodb_undo_tablespaces_implicit\t2\n" +
                "Innodb_undo_tablespaces_explicit\t0\n" +
                "Innodb_undo_tablespaces_active\t2\n" +
                "Key_blocks_not_flushed\t0\n" +
                "Key_blocks_unused\t7010\n" +
                "Key_blocks_used\t0\n" +
                "Key_read_requests\t0\n" +
                "Key_reads\t0\n" +
                "Key_write_requests\t0\n" +
                "Key_writes\t0\n" +
                "Last_query_cost\t0.000000\n" +
                "Last_query_partial_plans\t0\n" +
                "Locked_connects\t0\n" +
                "Max_execution_time_exceeded\t0\n" +
                "Max_execution_time_set\t0\n" +
                "Max_execution_time_set_failed\t0\n" +
                "Max_used_connections\t4\n" +
                "Max_used_connections_time\t2019-12-17 21:01:47\n" +
                "Mysqlx_aborted_clients\t0\n" +
                "Mysqlx_address\t::\n" +
                "Mysqlx_bytes_received\t0\n" +
                "Mysqlx_bytes_sent\t0\n" +
                "Mysqlx_connection_accept_errors\t0\n" +
                "Mysqlx_connection_errors\t0\n" +
                "Mysqlx_connections_accepted\t0\n" +
                "Mysqlx_connections_closed\t0\n" +
                "Mysqlx_connections_rejected\t0\n" +
                "Mysqlx_crud_create_view\t0\n" +
                "Mysqlx_crud_delete\t0\n" +
                "Mysqlx_crud_drop_view\t0\n" +
                "Mysqlx_crud_find\t0\n" +
                "Mysqlx_crud_insert\t0\n" +
                "Mysqlx_crud_modify_view\t0\n" +
                "Mysqlx_crud_update\t0\n" +
                "Mysqlx_cursor_close\t0\n" +
                "Mysqlx_cursor_fetch\t0\n" +
                "Mysqlx_cursor_open\t0\n" +
                "Mysqlx_errors_sent\t0\n" +
                "Mysqlx_errors_unknown_message_type\t0\n" +
                "Mysqlx_expect_close\t0\n" +
                "Mysqlx_expect_open\t0\n" +
                "Mysqlx_init_error\t0\n" +
                "Mysqlx_notice_global_sent\t0\n" +
                "Mysqlx_notice_other_sent\t0\n" +
                "Mysqlx_notice_warning_sent\t0\n" +
                "Mysqlx_notified_by_group_replication\t0\n" +
                "Mysqlx_port\t33060\n" +
                "Mysqlx_prep_deallocate\t0\n" +
                "Mysqlx_prep_execute\t0\n" +
                "Mysqlx_prep_prepare\t0\n" +
                "Mysqlx_rows_sent\t0\n" +
                "Mysqlx_sessions\t0\n" +
                "Mysqlx_sessions_accepted\t0\n" +
                "Mysqlx_sessions_closed\t0\n" +
                "Mysqlx_sessions_fatal_error\t0\n" +
                "Mysqlx_sessions_killed\t0\n" +
                "Mysqlx_sessions_rejected\t0\n" +
                "Mysqlx_socket\t/var/run/mysqld/mysqlx.sock\n" +
                "Mysqlx_ssl_accepts\t0\n" +
                "Mysqlx_ssl_active\t\n" +
                "Mysqlx_ssl_cipher\t\n" +
                "Mysqlx_ssl_cipher_list\t\n" +
                "Mysqlx_ssl_ctx_verify_depth\t18446744073709551615\n" +
                "Mysqlx_ssl_ctx_verify_mode\t5\n" +
                "Mysqlx_ssl_finished_accepts\t0\n" +
                "Mysqlx_ssl_server_not_after\tOct 15 06:46:11 2029 GMT\n" +
                "Mysqlx_ssl_server_not_before\tOct 18 06:46:11 2019 GMT\n" +
                "Mysqlx_ssl_verify_depth\t\n" +
                "Mysqlx_ssl_verify_mode\t\n" +
                "Mysqlx_ssl_version\t\n" +
                "Mysqlx_stmt_create_collection\t0\n" +
                "Mysqlx_stmt_create_collection_index\t0\n" +
                "Mysqlx_stmt_disable_notices\t0\n" +
                "Mysqlx_stmt_drop_collection\t0\n" +
                "Mysqlx_stmt_drop_collection_index\t0\n" +
                "Mysqlx_stmt_enable_notices\t0\n" +
                "Mysqlx_stmt_ensure_collection\t0\n" +
                "Mysqlx_stmt_execute_mysqlx\t0\n" +
                "Mysqlx_stmt_execute_sql\t0\n" +
                "Mysqlx_stmt_execute_xplugin\t0\n" +
                "Mysqlx_stmt_kill_client\t0\n" +
                "Mysqlx_stmt_list_clients\t0\n" +
                "Mysqlx_stmt_list_notices\t0\n" +
                "Mysqlx_stmt_list_objects\t0\n" +
                "Mysqlx_stmt_ping\t0\n" +
                "Mysqlx_worker_threads\t2\n" +
                "Mysqlx_worker_threads_active\t0\n" +
                "Not_flushed_delayed_rows\t0\n" +
                "Ongoing_anonymous_transaction_count\t0\n" +
                "Open_files\t3\n" +
                "Open_streams\t0\n" +
                "Open_table_definitions\t58\n" +
                "Open_tables\t272\n" +
                "Opened_files\t45\n" +
                "Opened_table_definitions\t0\n" +
                "Opened_tables\t0\n" +
                "Performance_schema_accounts_lost\t0\n" +
                "Performance_schema_cond_classes_lost\t0\n" +
                "Performance_schema_cond_instances_lost\t0\n" +
                "Performance_schema_digest_lost\t0\n" +
                "Performance_schema_file_classes_lost\t0\n" +
                "Performance_schema_file_handles_lost\t0\n" +
                "Performance_schema_file_instances_lost\t0\n" +
                "Performance_schema_hosts_lost\t0\n" +
                "Performance_schema_index_stat_lost\t0\n" +
                "Performance_schema_locker_lost\t0\n" +
                "Performance_schema_memory_classes_lost\t0\n" +
                "Performance_schema_metadata_lock_lost\t0\n" +
                "Performance_schema_mutex_classes_lost\t0\n" +
                "Performance_schema_mutex_instances_lost\t0\n" +
                "Performance_schema_nested_statement_lost\t0\n" +
                "Performance_schema_prepared_statements_lost\t0\n" +
                "Performance_schema_program_lost\t0\n" +
                "Performance_schema_rwlock_classes_lost\t0\n" +
                "Performance_schema_rwlock_instances_lost\t0\n" +
                "Performance_schema_session_connect_attrs_longest_seen\t115\n" +
                "Performance_schema_session_connect_attrs_lost\t0\n" +
                "Performance_schema_socket_classes_lost\t0\n" +
                "Performance_schema_socket_instances_lost\t0\n" +
                "Performance_schema_stage_classes_lost\t0\n" +
                "Performance_schema_statement_classes_lost\t0\n" +
                "Performance_schema_table_handles_lost\t0\n" +
                "Performance_schema_table_instances_lost\t0\n" +
                "Performance_schema_table_lock_stat_lost\t0\n" +
                "Performance_schema_thread_classes_lost\t0\n" +
                "Performance_schema_thread_instances_lost\t0\n" +
                "Performance_schema_users_lost\t0\n" +
                "Prepared_stmt_count\t0\n" +
                "Queries\t137\n" +
                "Questions\t2\n" +
                "Rsa_public_key\t-----BEGIN PUBLIC KEY-----\\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsnW6k9tEqLsHbKPf+lfP\\nNY5ZNLtLuzS9rxCVDwF3eI3Mi1WdfLUbKvkiHlM2BtDCwYwryCkEcRlXlvfStVam\\nRDVWhDauzFdFmAmeVMp4leGQiJ3JjWIaRLXlYRddfGBNG/7qkh2y8nm3gP5vgVa7\\nznhmlyEW/guH5Fiso3JByysvP68H3ptxAiJyykxl9GHBGxDVmSvTvveXQZi+xz5w\\nqHZIKHC5TRPKZvm3vFoDtoeIbt7VO7t0IC5NtbT31UM2eZBjvczLr332wc7ZDaqW\\n/AX482PpVGjAVD5TYY1lKIcYgJ8l2WYxO8qOZlbp4WDU9Ui7EuKuDoVYNtq4yiI4\\nhwIDAQAB\\n-----END PUBLIC KEY-----\\n\n" +
                "Secondary_engine_execution_count\t0\n" +
                "Select_full_join\t0\n" +
                "Select_full_range_join\t0\n" +
                "Select_range\t0\n" +
                "Select_range_check\t0\n" +
                "Select_scan\t0\n" +
                "Slave_open_temp_tables\t0\n" +
                "Slow_launch_threads\t0\n" +
                "Slow_queries\t0\n" +
                "Sort_merge_passes\t0\n" +
                "Sort_range\t0\n" +
                "Sort_rows\t0\n" +
                "Sort_scan\t0\n" +
                "Ssl_accept_renegotiates\t0\n" +
                "Ssl_accepts\t0\n" +
                "Ssl_callback_cache_hits\t0\n" +
                "Ssl_cipher\t\n" +
                "Ssl_cipher_list\t\n" +
                "Ssl_client_connects\t0\n" +
                "Ssl_connect_renegotiates\t0\n" +
                "Ssl_ctx_verify_depth\t18446744073709551615\n" +
                "Ssl_ctx_verify_mode\t5\n" +
                "Ssl_default_timeout\t0\n" +
                "Ssl_finished_accepts\t0\n" +
                "Ssl_finished_connects\t0\n" +
                "Ssl_server_not_after\tOct 15 06:46:11 2029 GMT\n" +
                "Ssl_server_not_before\tOct 18 06:46:11 2019 GMT\n" +
                "Ssl_session_cache_hits\t0\n" +
                "Ssl_session_cache_misses\t0\n" +
                "Ssl_session_cache_mode\tSERVER\n" +
                "Ssl_session_cache_overflows\t0\n" +
                "Ssl_session_cache_size\t128\n" +
                "Ssl_session_cache_timeouts\t0\n" +
                "Ssl_sessions_reused\t0\n" +
                "Ssl_used_session_cache_entries\t0\n" +
                "Ssl_verify_depth\t0\n" +
                "Ssl_verify_mode\t0\n" +
                "Ssl_version\t\n" +
                "Table_locks_immediate\t24\n" +
                "Table_locks_waited\t0\n" +
                "Table_open_cache_hits\t0\n" +
                "Table_open_cache_misses\t0\n" +
                "Table_open_cache_overflows\t0\n" +
                "Tc_log_max_pages_used\t0\n" +
                "Tc_log_page_size\t0\n" +
                "Tc_log_page_waits\t0\n" +
                "Threads_cached\t0\n" +
                "Threads_connected\t4\n" +
                "Threads_created\t4\n" +
                "Threads_running\t3\n" +
                "Uptime\t3837\n" +
                "Uptime_since_flush_status\t3837\n" +
                "EXPLAIN\n" +
                "-> Limit: 100 row(s)  (actual time=128.299..128.314 rows=64 loops=1)\\n    -> Sort: <temporary>.d_year, <temporary>.sum_agg DESC, <temporary>.i_brand_id, limit input to 100 row(s) per chunk  (actual time=128.298..128.307 rows=64 loops=1)\\n        -> Table scan on <temporary>  (actual time=0.002..0.008 rows=64 loops=1)\\n            -> Aggregate using temporary table  (actual time=128.178..128.192 rows=64 loops=1)\\n                -> Nested loop inner join  (cost=285687.56 rows=23684) (actual time=8.641..125.474 rows=925 loops=1)\\n                    -> Nested loop inner join  (cost=30317.96 rows=236842) (actual time=8.554..109.532 rows=5811 loops=1)\\n                        -> Filter: (item.i_manufact_id = 436)  (cost=3352.91 rows=2520) (actual time=7.795..75.730 rows=32 loops=1)\\n                            -> Table scan on item  (cost=3352.91 rows=25196) (actual time=0.090..72.972 rows=26000 loops=1)\\n                        -> Filter: (store_sales.ss_sold_date_sk is not null)  (cost=1.31 rows=94) (actual time=0.747..0.995 rows=182 loops=32)\\n                            -> Index lookup on store_sales using PRIMARY (ss_item_sk=item.i_item_sk)  (cost=1.31 rows=94) (actual time=0.746..0.968 rows=182 loops=32)\\n                    -> Filter: (dt.d_moy = 12)  (cost=0.98 rows=0) (actual time=0.002..0.002 rows=0 loops=5811)\\n                        -> Single-row index lookup on dt using PRIMARY (d_date_sk=store_sales.ss_sold_date_sk)  (cost=0.98 rows=1) (actual time=0.002..0.002 rows=1 loops=5811)\\n\n" +
                "Variable_name\tValue\n" +
                "Aborted_clients\t0\n" +
                "Aborted_connects\t0\n" +
                "Acl_cache_items_count\t0\n" +
                "Binlog_cache_disk_use\t0\n" +
                "Binlog_cache_use\t0\n" +
                "Binlog_stmt_cache_disk_use\t0\n" +
                "Binlog_stmt_cache_use\t0\n" +
                "Bytes_received\t268\n" +
                "Bytes_sent\t188\n" +
                "Caching_sha2_password_rsa_public_key\t-----BEGIN PUBLIC KEY-----\\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsnW6k9tEqLsHbKPf+lfP\\nNY5ZNLtLuzS9rxCVDwF3eI3Mi1WdfLUbKvkiHlM2BtDCwYwryCkEcRlXlvfStVam\\nRDVWhDauzFdFmAmeVMp4leGQiJ3JjWIaRLXlYRddfGBNG/7qkh2y8nm3gP5vgVa7\\nznhmlyEW/guH5Fiso3JByysvP68H3ptxAiJyykxl9GHBGxDVmSvTvveXQZi+xz5w\\nqHZIKHC5TRPKZvm3vFoDtoeIbt7VO7t0IC5NtbT31UM2eZBjvczLr332wc7ZDaqW\\n/AX482PpVGjAVD5TYY1lKIcYgJ8l2WYxO8qOZlbp4WDU9Ui7EuKuDoVYNtq4yiI4\\nhwIDAQAB\\n-----END PUBLIC KEY-----\\n\n" +
                "Com_admin_commands\t0\n" +
                "Com_assign_to_keycache\t0\n" +
                "Com_alter_db\t0\n" +
                "Com_alter_event\t0\n" +
                "Com_alter_function\t0\n" +
                "Com_alter_instance\t0\n" +
                "Com_alter_procedure\t0\n" +
                "Com_alter_resource_group\t0\n" +
                "Com_alter_server\t0\n" +
                "Com_alter_table\t0\n" +
                "Com_alter_tablespace\t0\n" +
                "Com_alter_user\t0\n" +
                "Com_alter_user_default_role\t0\n" +
                "Com_analyze\t0\n" +
                "Com_begin\t0\n" +
                "Com_binlog\t0\n" +
                "Com_call_procedure\t0\n" +
                "Com_change_db\t0\n" +
                "Com_change_master\t0\n" +
                "Com_change_repl_filter\t0\n" +
                "Com_check\t0\n" +
                "Com_checksum\t0\n" +
                "Com_clone\t0\n" +
                "Com_commit\t0\n" +
                "Com_create_db\t0\n" +
                "Com_create_event\t0\n" +
                "Com_create_function\t0\n" +
                "Com_create_index\t0\n" +
                "Com_create_procedure\t0\n" +
                "Com_create_role\t0\n" +
                "Com_create_server\t0\n" +
                "Com_create_table\t0\n" +
                "Com_create_resource_group\t0\n" +
                "Com_create_trigger\t0\n" +
                "Com_create_udf\t0\n" +
                "Com_create_user\t0\n" +
                "Com_create_view\t0\n" +
                "Com_create_spatial_reference_system\t0\n" +
                "Com_dealloc_sql\t0\n" +
                "Com_delete\t0\n" +
                "Com_delete_multi\t0\n" +
                "Com_do\t0\n" +
                "Com_drop_db\t0\n" +
                "Com_drop_event\t0\n" +
                "Com_drop_function\t0\n" +
                "Com_drop_index\t0\n" +
                "Com_drop_procedure\t0\n" +
                "Com_drop_resource_group\t0\n" +
                "Com_drop_role\t0\n" +
                "Com_drop_server\t0\n" +
                "Com_drop_spatial_reference_system\t0\n" +
                "Com_drop_table\t0\n" +
                "Com_drop_trigger\t0\n" +
                "Com_drop_user\t0\n" +
                "Com_drop_view\t0\n" +
                "Com_empty_query\t0\n" +
                "Com_execute_sql\t0\n" +
                "Com_explain_other\t0\n" +
                "Com_flush\t0\n" +
                "Com_get_diagnostics\t0\n" +
                "Com_grant\t0\n" +
                "Com_grant_roles\t0\n" +
                "Com_ha_close\t0\n" +
                "Com_ha_open\t0\n" +
                "Com_ha_read\t0\n" +
                "Com_help\t0\n" +
                "Com_import\t0\n" +
                "Com_insert\t0\n" +
                "Com_insert_select\t0\n" +
                "Com_install_component\t0\n" +
                "Com_install_plugin\t0\n" +
                "Com_kill\t0\n" +
                "Com_load\t0\n" +
                "Com_lock_instance\t0\n" +
                "Com_lock_tables\t0\n" +
                "Com_optimize\t0\n" +
                "Com_preload_keys\t0\n" +
                "Com_prepare_sql\t0\n" +
                "Com_purge\t0\n" +
                "Com_purge_before_date\t0\n" +
                "Com_release_savepoint\t0\n" +
                "Com_rename_table\t0\n" +
                "Com_rename_user\t0\n" +
                "Com_repair\t0\n" +
                "Com_replace\t0\n" +
                "Com_replace_select\t0\n" +
                "Com_reset\t0\n" +
                "Com_resignal\t0\n" +
                "Com_restart\t0\n" +
                "Com_revoke\t0\n" +
                "Com_revoke_all\t0\n" +
                "Com_revoke_roles\t0\n" +
                "Com_rollback\t0\n" +
                "Com_rollback_to_savepoint\t0\n" +
                "Com_savepoint\t0\n" +
                "Com_select\t1\n" +
                "Com_set_option\t0\n" +
                "Com_set_password\t0\n" +
                "Com_set_resource_group\t0\n" +
                "Com_set_role\t0\n" +
                "Com_signal\t0\n" +
                "Com_show_binlog_events\t0\n" +
                "Com_show_binlogs\t0\n" +
                "Com_show_charsets\t0\n" +
                "Com_show_collations\t0\n" +
                "Com_show_create_db\t0\n" +
                "Com_show_create_event\t0\n" +
                "Com_show_create_func\t0\n" +
                "Com_show_create_proc\t0\n" +
                "Com_show_create_table\t0\n" +
                "Com_show_create_trigger\t0\n" +
                "Com_show_databases\t0\n" +
                "Com_show_engine_logs\t0\n" +
                "Com_show_engine_mutex\t0\n" +
                "Com_show_engine_status\t0\n" +
                "Com_show_events\t0\n" +
                "Com_show_errors\t0\n" +
                "Com_show_fields\t0\n" +
                "Com_show_function_code\t0\n" +
                "Com_show_function_status\t0\n" +
                "Com_show_grants\t0\n" +
                "Com_show_keys\t0\n" +
                "Com_show_master_status\t0\n" +
                "Com_show_open_tables\t0\n" +
                "Com_show_plugins\t0\n" +
                "Com_show_privileges\t0\n" +
                "Com_show_procedure_code\t0\n" +
                "Com_show_procedure_status\t0\n" +
                "Com_show_processlist\t0\n" +
                "Com_show_profile\t0\n" +
                "Com_show_profiles\t0\n" +
                "Com_show_relaylog_events\t0\n" +
                "Com_show_slave_hosts\t0\n" +
                "Com_show_slave_status\t0\n" +
                "Com_show_status\t1\n" +
                "Com_show_storage_engines\t0\n" +
                "Com_show_table_status\t0\n" +
                "Com_show_tables\t0\n" +
                "Com_show_triggers\t0\n" +
                "Com_show_variables\t0\n" +
                "Com_show_warnings\t0\n" +
                "Com_show_create_user\t0\n" +
                "Com_shutdown\t0\n" +
                "Com_slave_start\t0\n" +
                "Com_slave_stop\t0\n" +
                "Com_group_replication_start\t0\n" +
                "Com_group_replication_stop\t0\n" +
                "Com_stmt_execute\t0\n" +
                "Com_stmt_close\t0\n" +
                "Com_stmt_fetch\t0\n" +
                "Com_stmt_prepare\t0\n" +
                "Com_stmt_reset\t0\n" +
                "Com_stmt_send_long_data\t0\n" +
                "Com_truncate\t0\n" +
                "Com_uninstall_component\t0\n" +
                "Com_uninstall_plugin\t0\n" +
                "Com_unlock_instance\t0\n" +
                "Com_unlock_tables\t0\n" +
                "Com_update\t0\n" +
                "Com_update_multi\t0\n" +
                "Com_xa_commit\t0\n" +
                "Com_xa_end\t0\n" +
                "Com_xa_prepare\t0\n" +
                "Com_xa_recover\t0\n" +
                "Com_xa_rollback\t0\n" +
                "Com_xa_start\t0\n" +
                "Com_stmt_reprepare\t0\n" +
                "Compression\tOFF\n" +
                "Compression_algorithm\t\n" +
                "Compression_level\t0\n" +
                "Connection_errors_accept\t0\n" +
                "Connection_errors_internal\t0\n" +
                "Connection_errors_max_connections\t0\n" +
                "Connection_errors_peer_address\t0\n" +
                "Connection_errors_select\t0\n" +
                "Connection_errors_tcpwrap\t0\n" +
                "Connections\t56\n" +
                "Created_tmp_disk_tables\t0\n" +
                "Created_tmp_files\t21\n" +
                "Created_tmp_tables\t0\n" +
                "Current_tls_ca\tca.pem\n" +
                "Current_tls_capath\t\n" +
                "Current_tls_cert\tserver-cert.pem\n" +
                "Current_tls_cipher\t\n" +
                "Current_tls_ciphersuites\t\n" +
                "Current_tls_crl\t\n" +
                "Current_tls_crlpath\t\n" +
                "Current_tls_key\tserver-key.pem\n" +
                "Current_tls_version\tTLSv1,TLSv1.1,TLSv1.2,TLSv1.3\n" +
                "Delayed_errors\t0\n" +
                "Delayed_insert_threads\t0\n" +
                "Delayed_writes\t0\n" +
                "Flush_commands\t3\n" +
                "Handler_commit\t0\n" +
                "Handler_delete\t0\n" +
                "Handler_discover\t0\n" +
                "Handler_external_lock\t0\n" +
                "Handler_mrr_init\t0\n" +
                "Handler_prepare\t0\n" +
                "Handler_read_first\t0\n" +
                "Handler_read_key\t0\n" +
                "Handler_read_last\t0\n" +
                "Handler_read_next\t0\n" +
                "Handler_read_prev\t0\n" +
                "Handler_read_rnd\t0\n" +
                "Handler_read_rnd_next\t0\n" +
                "Handler_rollback\t0\n" +
                "Handler_savepoint\t0\n" +
                "Handler_savepoint_rollback\t0\n" +
                "Handler_update\t0\n" +
                "Handler_write\t0\n" +
                "Innodb_buffer_pool_dump_status\tDumping of buffer pool not started\n" +
                "Innodb_buffer_pool_load_status\tBuffer pool(s) load completed at 191217 20:00:26\n" +
                "Innodb_buffer_pool_resize_status\t\n" +
                "Innodb_buffer_pool_pages_data\t7734\n" +
                "Innodb_buffer_pool_bytes_data\t126713856\n" +
                "Innodb_buffer_pool_pages_dirty\t0\n" +
                "Innodb_buffer_pool_bytes_dirty\t0\n" +
                "Innodb_buffer_pool_pages_flushed\t152\n" +
                "Innodb_buffer_pool_pages_free\t0\n" +
                "Innodb_buffer_pool_pages_misc\t458\n" +
                "Innodb_buffer_pool_pages_total\t8192\n" +
                "Innodb_buffer_pool_read_ahead_rnd\t0\n" +
                "Innodb_buffer_pool_read_ahead\t28940\n" +
                "Innodb_buffer_pool_read_ahead_evicted\t1012\n" +
                "Innodb_buffer_pool_read_requests\t67909029\n" +
                "Innodb_buffer_pool_reads\t945553\n" +
                "Innodb_buffer_pool_wait_free\t0\n" +
                "Innodb_buffer_pool_write_requests\t1640\n" +
                "Innodb_data_fsyncs\t61\n" +
                "Innodb_data_pending_fsyncs\t0\n" +
                "Innodb_data_pending_reads\t1\n" +
                "Innodb_data_pending_writes\t0\n" +
                "Innodb_data_read\t15966180352\n" +
                "Innodb_data_reads\t974545\n" +
                "Innodb_data_writes\t231\n" +
                "Innodb_data_written\t3057664\n" +
                "Innodb_dblwr_pages_written\t21\n" +
                "Innodb_dblwr_writes\t8\n" +
                "Innodb_log_waits\t0\n" +
                "Innodb_log_write_requests\t647\n" +
                "Innodb_log_writes\t20\n" +
                "Innodb_os_log_fsyncs\t21\n" +
                "Innodb_os_log_pending_fsyncs\t0\n" +
                "Innodb_os_log_pending_writes\t0\n" +
                "Innodb_os_log_written\t36352\n" +
                "Innodb_page_size\t16384\n" +
                "Innodb_pages_created\t142\n" +
                "Innodb_pages_read\t974493\n" +
                "Innodb_pages_written\t162\n" +
                "Innodb_row_lock_current_waits\t0\n" +
                "Innodb_row_lock_time\t0\n" +
                "Innodb_row_lock_time_avg\t0\n" +
                "Innodb_row_lock_time_max\t0\n" +
                "Innodb_row_lock_waits\t0\n" +
                "Innodb_rows_deleted\t0\n" +
                "Innodb_rows_inserted\t0\n" +
                "Innodb_rows_read\t103376531\n" +
                "Innodb_rows_updated\t315\n" +
                "Innodb_num_open_files\t31\n" +
                "Innodb_truncated_status_writes\t0\n" +
                "Innodb_undo_tablespaces_total\t2\n" +
                "Innodb_undo_tablespaces_implicit\t2\n" +
                "Innodb_undo_tablespaces_explicit\t0\n" +
                "Innodb_undo_tablespaces_active\t2\n" +
                "Key_blocks_not_flushed\t0\n" +
                "Key_blocks_unused\t7010\n" +
                "Key_blocks_used\t0\n" +
                "Key_read_requests\t0\n" +
                "Key_reads\t0\n" +
                "Key_write_requests\t0\n" +
                "Key_writes\t0\n" +
                "Last_query_cost\t0.000000\n" +
                "Last_query_partial_plans\t0\n" +
                "Locked_connects\t0\n" +
                "Max_execution_time_exceeded\t0\n" +
                "Max_execution_time_set\t0\n" +
                "Max_execution_time_set_failed\t0\n" +
                "Max_used_connections\t4\n" +
                "Max_used_connections_time\t2019-12-17 21:01:47\n" +
                "Mysqlx_aborted_clients\t0\n" +
                "Mysqlx_address\t::\n" +
                "Mysqlx_bytes_received\t0\n" +
                "Mysqlx_bytes_sent\t0\n" +
                "Mysqlx_connection_accept_errors\t0\n" +
                "Mysqlx_connection_errors\t0\n" +
                "Mysqlx_connections_accepted\t0\n" +
                "Mysqlx_connections_closed\t0\n" +
                "Mysqlx_connections_rejected\t0\n" +
                "Mysqlx_crud_create_view\t0\n" +
                "Mysqlx_crud_delete\t0\n" +
                "Mysqlx_crud_drop_view\t0\n" +
                "Mysqlx_crud_find\t0\n" +
                "Mysqlx_crud_insert\t0\n" +
                "Mysqlx_crud_modify_view\t0\n" +
                "Mysqlx_crud_update\t0\n" +
                "Mysqlx_cursor_close\t0\n" +
                "Mysqlx_cursor_fetch\t0\n" +
                "Mysqlx_cursor_open\t0\n" +
                "Mysqlx_errors_sent\t0\n" +
                "Mysqlx_errors_unknown_message_type\t0\n" +
                "Mysqlx_expect_close\t0\n" +
                "Mysqlx_expect_open\t0\n" +
                "Mysqlx_init_error\t0\n" +
                "Mysqlx_notice_global_sent\t0\n" +
                "Mysqlx_notice_other_sent\t0\n" +
                "Mysqlx_notice_warning_sent\t0\n" +
                "Mysqlx_notified_by_group_replication\t0\n" +
                "Mysqlx_port\t33060\n" +
                "Mysqlx_prep_deallocate\t0\n" +
                "Mysqlx_prep_execute\t0\n" +
                "Mysqlx_prep_prepare\t0\n" +
                "Mysqlx_rows_sent\t0\n" +
                "Mysqlx_sessions\t0\n" +
                "Mysqlx_sessions_accepted\t0\n" +
                "Mysqlx_sessions_closed\t0\n" +
                "Mysqlx_sessions_fatal_error\t0\n" +
                "Mysqlx_sessions_killed\t0\n" +
                "Mysqlx_sessions_rejected\t0\n" +
                "Mysqlx_socket\t/var/run/mysqld/mysqlx.sock\n" +
                "Mysqlx_ssl_accepts\t0\n" +
                "Mysqlx_ssl_active\t\n" +
                "Mysqlx_ssl_cipher\t\n" +
                "Mysqlx_ssl_cipher_list\t\n" +
                "Mysqlx_ssl_ctx_verify_depth\t18446744073709551615\n" +
                "Mysqlx_ssl_ctx_verify_mode\t5\n" +
                "Mysqlx_ssl_finished_accepts\t0\n" +
                "Mysqlx_ssl_server_not_after\tOct 15 06:46:11 2029 GMT\n" +
                "Mysqlx_ssl_server_not_before\tOct 18 06:46:11 2019 GMT\n" +
                "Mysqlx_ssl_verify_depth\t\n" +
                "Mysqlx_ssl_verify_mode\t\n" +
                "Mysqlx_ssl_version\t\n" +
                "Mysqlx_stmt_create_collection\t0\n" +
                "Mysqlx_stmt_create_collection_index\t0\n" +
                "Mysqlx_stmt_disable_notices\t0\n" +
                "Mysqlx_stmt_drop_collection\t0\n" +
                "Mysqlx_stmt_drop_collection_index\t0\n" +
                "Mysqlx_stmt_enable_notices\t0\n" +
                "Mysqlx_stmt_ensure_collection\t0\n" +
                "Mysqlx_stmt_execute_mysqlx\t0\n" +
                "Mysqlx_stmt_execute_sql\t0\n" +
                "Mysqlx_stmt_execute_xplugin\t0\n" +
                "Mysqlx_stmt_kill_client\t0\n" +
                "Mysqlx_stmt_list_clients\t0\n" +
                "Mysqlx_stmt_list_notices\t0\n" +
                "Mysqlx_stmt_list_objects\t0\n" +
                "Mysqlx_stmt_ping\t0\n" +
                "Mysqlx_worker_threads\t2\n" +
                "Mysqlx_worker_threads_active\t0\n" +
                "Not_flushed_delayed_rows\t0\n" +
                "Ongoing_anonymous_transaction_count\t0\n" +
                "Open_files\t3\n" +
                "Open_streams\t0\n" +
                "Open_table_definitions\t58\n" +
                "Open_tables\t272\n" +
                "Opened_files\t45\n" +
                "Opened_table_definitions\t0\n" +
                "Opened_tables\t0\n" +
                "Performance_schema_accounts_lost\t0\n" +
                "Performance_schema_cond_classes_lost\t0\n" +
                "Performance_schema_cond_instances_lost\t0\n" +
                "Performance_schema_digest_lost\t0\n" +
                "Performance_schema_file_classes_lost\t0\n" +
                "Performance_schema_file_handles_lost\t0\n" +
                "Performance_schema_file_instances_lost\t0\n" +
                "Performance_schema_hosts_lost\t0\n" +
                "Performance_schema_index_stat_lost\t0\n" +
                "Performance_schema_locker_lost\t0\n" +
                "Performance_schema_memory_classes_lost\t0\n" +
                "Performance_schema_metadata_lock_lost\t0\n" +
                "Performance_schema_mutex_classes_lost\t0\n" +
                "Performance_schema_mutex_instances_lost\t0\n" +
                "Performance_schema_nested_statement_lost\t0\n" +
                "Performance_schema_prepared_statements_lost\t0\n" +
                "Performance_schema_program_lost\t0\n" +
                "Performance_schema_rwlock_classes_lost\t0\n" +
                "Performance_schema_rwlock_instances_lost\t0\n" +
                "Performance_schema_session_connect_attrs_longest_seen\t115\n" +
                "Performance_schema_session_connect_attrs_lost\t0\n" +
                "Performance_schema_socket_classes_lost\t0\n" +
                "Performance_schema_socket_instances_lost\t0\n" +
                "Performance_schema_stage_classes_lost\t0\n" +
                "Performance_schema_statement_classes_lost\t0\n" +
                "Performance_schema_table_handles_lost\t0\n" +
                "Performance_schema_table_instances_lost\t0\n" +
                "Performance_schema_table_lock_stat_lost\t0\n" +
                "Performance_schema_thread_classes_lost\t0\n" +
                "Performance_schema_thread_instances_lost\t0\n" +
                "Performance_schema_users_lost\t0\n" +
                "Prepared_stmt_count\t0\n" +
                "Queries\t143\n" +
                "Questions\t2\n" +
                "Rsa_public_key\t-----BEGIN PUBLIC KEY-----\\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsnW6k9tEqLsHbKPf+lfP\\nNY5ZNLtLuzS9rxCVDwF3eI3Mi1WdfLUbKvkiHlM2BtDCwYwryCkEcRlXlvfStVam\\nRDVWhDauzFdFmAmeVMp4leGQiJ3JjWIaRLXlYRddfGBNG/7qkh2y8nm3gP5vgVa7\\nznhmlyEW/guH5Fiso3JByysvP68H3ptxAiJyykxl9GHBGxDVmSvTvveXQZi+xz5w\\nqHZIKHC5TRPKZvm3vFoDtoeIbt7VO7t0IC5NtbT31UM2eZBjvczLr332wc7ZDaqW\\n/AX482PpVGjAVD5TYY1lKIcYgJ8l2WYxO8qOZlbp4WDU9Ui7EuKuDoVYNtq4yiI4\\nhwIDAQAB\\n-----END PUBLIC KEY-----\\n\n" +
                "Secondary_engine_execution_count\t0\n" +
                "Select_full_join\t0\n" +
                "Select_full_range_join\t0\n" +
                "Select_range\t0\n" +
                "Select_range_check\t0\n" +
                "Select_scan\t0\n" +
                "Slave_open_temp_tables\t0\n" +
                "Slow_launch_threads\t0\n" +
                "Slow_queries\t0\n" +
                "Sort_merge_passes\t0\n" +
                "Sort_range\t0\n" +
                "Sort_rows\t0\n" +
                "Sort_scan\t0\n" +
                "Ssl_accept_renegotiates\t0\n" +
                "Ssl_accepts\t0\n" +
                "Ssl_callback_cache_hits\t0\n" +
                "Ssl_cipher\t\n" +
                "Ssl_cipher_list\t\n" +
                "Ssl_client_connects\t0\n" +
                "Ssl_connect_renegotiates\t0\n" +
                "Ssl_ctx_verify_depth\t18446744073709551615\n" +
                "Ssl_ctx_verify_mode\t5\n" +
                "Ssl_default_timeout\t0\n" +
                "Ssl_finished_accepts\t0\n" +
                "Ssl_finished_connects\t0\n" +
                "Ssl_server_not_after\tOct 15 06:46:11 2029 GMT\n" +
                "Ssl_server_not_before\tOct 18 06:46:11 2019 GMT\n" +
                "Ssl_session_cache_hits\t0\n" +
                "Ssl_session_cache_misses\t0\n" +
                "Ssl_session_cache_mode\tSERVER\n" +
                "Ssl_session_cache_overflows\t0\n" +
                "Ssl_session_cache_size\t128\n" +
                "Ssl_session_cache_timeouts\t0\n" +
                "Ssl_sessions_reused\t0\n" +
                "Ssl_used_session_cache_entries\t0\n" +
                "Ssl_verify_depth\t0\n" +
                "Ssl_verify_mode\t0\n" +
                "Ssl_version\t\n" +
                "Table_locks_immediate\t25\n" +
                "Table_locks_waited\t0\n" +
                "Table_open_cache_hits\t0\n" +
                "Table_open_cache_misses\t0\n" +
                "Table_open_cache_overflows\t0\n" +
                "Tc_log_max_pages_used\t0\n" +
                "Tc_log_page_size\t0\n" +
                "Tc_log_page_waits\t0\n" +
                "Threads_cached\t0\n" +
                "Threads_connected\t4\n" +
                "Threads_created\t4\n" +
                "Threads_running\t3\n" +
                "Uptime\t3837\n" +
                "Uptime_since_flush_status\t3837";
        HashMap<String, Double> result = processPeformance(performString);
//        HashMap<String, ArrayList<Double>> result1 = process(queryPlanString);
    }
}
