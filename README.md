# Probes Generator (PG)

This is a software suite that allows the generation of millions of DNA barcodes (probes) that are used for the [RQPAP](https://github.com/alexelshaikh/RQPAP) software suite.
This probe generator program utilizes locality-sensitive hashing (LSH) to create sufficiently specific probes.

## Installation

Make sure you have [Java 16+](https://www.oracle.com/de/java/technologies/javase-downloads.html) and [Maven](https://maven.apache.org/download.cgi) installed. To build the project, run the following command in the root directory of this project.
```sh
mvn package
```
Alternatively, you can import this project into your IDE as a **maven project** and run maven `package` from there.
The output executable _.jar_ file will be located at `probes-generator/target/pg-1.0.jar`.




## Usage

The PG requires setting the correct parameters to start generating. Parameters can be set from the console as such:
`parameter_name=parameter_value`. For example, `count=100` will set the parameter `count` to `100`.

Note that the PG will list **all** the parameters (with default values if not set). You will be asked to approve the correctness of the parameters (unless you set `approve=false`) before generating the probes. To confirm, you will have to type "y" on the console and hit enter. See the next list of parameters to customize the pipeline.

### Parameters:

`count`: the number of probes that will be generated.

`len`: the length (bps) of each probe. All probes will have exactly the same length.

`threads`: the number of threads that will be used (not counting threads used to parallelize distance checks).

`gc`: target _GC_ content for each probe.

`d_dg`: maximum allowed deviation from `gc`. For example, if `gc=0.5` and `d_gc=0.1`, then the target _GC_ bound is [0.4, 0.6].

`gen_type`: _safe_gc_ to guarantee each sequence to be **exactly** within specified _GC_ bound or _prob_gc_ to achieve specified _GC_ bound probabilistically. _prob_gc_ can produce sequences that have a slightly higher or lower _GC_ content but might perform faster.

`dist_check`: _LSH_ to enable similarity (or distance) checks by LSH and _NAIVE_ to force the jaccard distance to be evaluated between a newly generated sequence and all the sequences computed so far.

`min_dist`: the minimum distance required for a probe to all other probes.

`lsh_k`: _k_-mer length used for LSH.

`lsh_r`: number _r_ of hash functions used for LSH.

`lsh_b`: number _b_ of bands used for LSH.

`save`: _true_ to save generated probes and _false_ to discard the generated probes. Default is _true_.

`save_path`: file path to save the generated probes. Default is "probes.fa".

`save_append`: _true_ to append the generated probes to `save_path` and _false_ to override `save_path`. Default is _false_.

`print_counter`: _true_ to print the current number of probes generated to the console and _false_ to ignore the counter. Default is _true_.

`counter_step`: the step size for `print_counter`. For example, if `counter_step=100` and `count=1000`, the program will print the current number of probes every 100 sequences, resulting in a total of 10 prints. If not set, the default value will be set as `counter_step=count/100`.

`max_err`: maximum error value allowed for a probe according to the DNA constraints given. See `DNARule.java` and `BasicDNARules.java`.

`use_dg_server`: _true_ to check for complex secondary structures, else _false_. To enable it, you have to start the python script `server.py` (see below).

`approve`: _true_ to require the approval of parameters before the generation of probes and _false_ to ignore this approval. When set _true_, the user will be asked to enter "y" followed by a return to confirm to start generating probes.

### Example

```sh
java -jar pg-1.0.jar count=1000
```
If you are computing a large number of probes, consider [increasing the available heap space of the JVM](https://docs.oracle.com/cd/E29587_01/PlatformServices.60x/ps_rel_discovery/src/crd_advanced_jvm_heap.html). The following command is equivalent to the one above but allows the JVM to use up to 20 GB of heap space.
```sh
java -jar -Xmx20g pg-1.0.jar count=1000
```

## Secondary Structure Prediction (`use_dg_server`)

If you wish to set `use_dg_server=true`, you will have to start the [Python 3](https://www.python.org/downloads/) script `server.py` in the directory `dg` beforehand. This script requires [seqfold](https://github.com/Lattice-Automation/seqfold) to be installed. Run the following command to install `seqfold`.
```sh
pip install seqfold
```
Then, to start the server from the root directory of this project, run the following command.
```sh
python dg/server.py
```

The server will automatically start on port 6000. For each additionally available thread, a new port will be used after 6000. For example, if your machine supports 4 threads, the server will use the following ports: 6000, 6001, 6002, and 6003. The PG will use all available ports.