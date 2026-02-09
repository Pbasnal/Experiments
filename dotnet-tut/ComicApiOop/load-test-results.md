## OOP Results
k6 run --env API_URL=http://localhost:8080 k6/load-test.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
/  \/    \    | |/ /  /   ‾‾\
/          \   |   (  |  (‾)  |
/ __________ \  |_|\_\  \_____/

     execution: local
        script: k6/load-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 20 max VUs, 4m0s max duration (incl. graceful stop):
              * default: Up to 20 looping VUs for 3m30s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)



█ THRESHOLDS

    comic_visibility_computation_duration
    ✓ 'p(95)<1000' p(95)=159ms

    errors
    ✓ 'rate<0.1' rate=0.00%

    http_req_duration
    ✓ 'p(95)<500' p(95)=148.92ms

      {endpoint:health}
      ✓ 'p(99)<100' p(99)=5.27ms


█ TOTAL RESULTS

    checks_total.......: 5560    25.849124/s
    checks_succeeded...: 100.00% 5560 out of 5560
    checks_failed......: 0.00%   0 out of 5560

    ✓ compute bulk status is 200
    ✓ compute bulk not timeout
    ✓ compute bulk has results
    ✓ compute bulk has computed visibilities
    ✓ compute bulk processed count matches limit
    ✓ bulk computation duration is reasonable
    ✓ invalid request returns 400
    ✓ compute single status is 200
    ✓ compute single not timeout
    ✓ compute single has results
    ✓ compute single has computed visibilities
    ✓ computation duration is reasonable
    ✓ health check status is 200

    CUSTOM
    comic_visibility_computation_duration...: avg=53.28ms min=15ms     med=24ms    max=204ms    p(90)=139ms    p(95)=159ms   
    errors..................................: 0.00%  0 out of 0

    HTTP
    http_req_duration.......................: avg=38.81ms min=503.29µs med=20.19ms max=204.04ms p(90)=126.13ms p(95)=148.92ms
      { endpoint:health }...................: avg=1.56ms  min=503.7µs  med=1.5ms   max=9.09ms   p(90)=2.14ms   p(95)=2.47ms  
      { expected_response:true }............: avg=42.87ms min=503.7µs  med=21.2ms  max=204.04ms p(90)=131.26ms p(95)=155.15ms
    http_req_failed.........................: 9.87%  131 out of 1327
    http_reqs...............................: 1327   6.169386/s

    EXECUTION
    iteration_duration......................: avg=2.05s   min=0s       med=2.01s   max=7.17s    p(90)=5.09s    p(95)=5.17s   
    iterations..............................: 1334   6.20193/s
    vus.....................................: 1      min=1           max=20
    vus_max.................................: 20     min=20          max=20

    NETWORK
    data_received...........................: 6.7 MB 31 kB/s
    data_sent...............................: 149 kB 692 B/s

k6 run --env API_URL=http://localhost:8080 k6/load-test.js                                                                     

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: k6/load-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 20 max VUs, 4m0s max duration (incl. graceful stop):
              * default: Up to 20 looping VUs for 3m30s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS

    comic_visibility_computation_duration
    ✓ 'p(95)<1000' p(95)=200.84ms

    errors
    ✓ 'rate<0.1' rate=0.00%

    http_req_duration
    ✓ 'p(95)<500' p(95)=187.01ms

      {endpoint:health}
      ✓ 'p(99)<100' p(99)=7.02ms


  █ TOTAL RESULTS

    checks_total.......: 5513    25.976494/s
    checks_succeeded...: 100.00% 5513 out of 5513
    checks_failed......: 0.00%   0 out of 5513

    ✓ health check status is 200
    ✓ compute bulk status is 200
    ✓ compute bulk not timeout
    ✓ compute bulk has results
    ✓ compute bulk has computed visibilities
    ✓ compute bulk processed count matches limit
    ✓ bulk computation duration is reasonable
    ✓ compute single status is 200
    ✓ compute single not timeout
    ✓ compute single has results
    ✓ compute single has computed visibilities
    ✓ computation duration is reasonable
    ✓ invalid request returns 400

    CUSTOM
    comic_visibility_computation_duration...: avg=62.86ms min=14ms    med=27ms    max=480ms    p(90)=174ms    p(95)=200.84ms
    errors..................................: 0.00%  0 out of 0

    HTTP
    http_req_duration.......................: avg=43.71ms min=502.2µs med=21.38ms max=479.24ms p(90)=149.6ms  p(95)=187.01ms
      { endpoint:health }...................: avg=1.79ms  min=502.9µs med=1.43ms  max=56.57ms  p(90)=2.39ms   p(95)=3.01ms
      { expected_response:true }............: avg=48.65ms min=502.9µs med=22.95ms max=479.24ms p(90)=156.29ms p(95)=192.27ms
    http_req_failed.........................: 10.48% 144 out of 1373
    http_reqs...............................: 1373   6.469386/s

    EXECUTION
    iteration_duration......................: avg=1.99s   min=0s      med=2.02s   max=7.22s    p(90)=5.08s    p(95)=5.2s
    iterations..............................: 1373   6.469386/s
    vus.....................................: 1      min=1           max=20
    vus_max.................................: 20     min=20          max=20

    NETWORK
    data_received...........................: 6.5 MB 31 kB/s
    data_sent...............................: 152 kB 718 B/s



                                                                                                                                               
running (3m32.2s), 00/20 VUs, 1373 complete and 0 interrupted iterations                                                                       
default ✓ [======================================] 00/20 VUs  3m30s    


## DOD Results
k6 run --env API_URL=http://localhost:8081 k6/load-test.js

         /\      Grafana   /‾‾/                                                                                    
    /\  /  \     |\  __   /  /                                                                                     
/  \/    \    | |/ /  /   ‾‾\                                                                                   
/          \   |   (  |  (‾)  |                                                                                  
/ __________ \  |_|\_\  \_____/

     execution: local
        script: k6/load-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 20 max VUs, 4m0s max duration (incl. graceful stop):
              * default: Up to 20 looping VUs for 3m30s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)



█ THRESHOLDS

    comic_visibility_computation_duration
    ✓ 'p(95)<1000' p(95)=570ms

    errors
    ✓ 'rate<0.1' rate=0.00%

    http_req_duration
    ✗ 'p(95)<500' p(95)=566.09ms

      {endpoint:health}
      ✓ 'p(99)<100' p(99)=3.53ms


█ TOTAL RESULTS

    checks_total.......: 4748    22.402705/s
    checks_succeeded...: 100.00% 4748 out of 4748
    checks_failed......: 0.00%   0 out of 4748

    ✓ health check status is 200
    ✓ compute bulk status is 200
    ✓ compute bulk not timeout
    ✓ compute bulk has results
    ✓ compute bulk has computed visibilities
    ✓ compute bulk processed count matches limit
    ✓ bulk computation duration is reasonable
    ✓ compute single status is 200
    ✓ compute single not timeout
    ✓ compute single has results
    ✓ compute single has computed visibilities
    ✓ computation duration is reasonable
    ✓ invalid request returns 400

    CUSTOM
    comic_visibility_computation_duration...: avg=496.35ms min=100ms    med=497.5ms  max=919ms   p(90)=557ms    p(95)=570ms   
    errors..................................: 0.00%  0 out of 0

    HTTP
    http_req_duration.......................: avg=345.29ms min=503.29µs med=477.45ms max=918.3ms p(90)=549.31ms p(95)=566.09ms
      { endpoint:health }...................: avg=1.44ms   min=504.1µs  med=1.26ms   max=21.36ms p(90)=2.04ms   p(95)=2.37ms  
      { expected_response:true }............: avg=390.59ms min=504.1µs  med=487.05ms max=918.3ms p(90)=552.31ms p(95)=567.34ms
    http_req_failed.........................: 11.63% 135 out of 1160
    http_reqs...............................: 1160   5.473281/s

    EXECUTION
    iteration_duration......................: avg=2.34s    min=0s       med=2.46s    max=8.02s   p(90)=5.98s    p(95)=6.06s   
    iterations..............................: 1166   5.501591/s
    vus.....................................: 1      min=1           max=20
    vus_max.................................: 20     min=20          max=20

    NETWORK
    data_received...........................: 5.6 MB 27 kB/s
    data_sent...............................: 130 kB 612 B/s